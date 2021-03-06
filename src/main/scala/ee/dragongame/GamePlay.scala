package ee.dragongame

import java.net.URL
import java.time.{Duration, Instant}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import ee.dragongame.elements.{Dragon, Game, GameResult}
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

import scala.io.Source

case class DBKey(agility: Int, armor: Int, attack: Int, endurance: Int, weather: WeatherCode)

object GamePlay extends StrictLogging {
  val config = ConfigFactory.load(this.getClass.getClassLoader)

  val learn = if (config.hasPath("learn")) {
    config.getBoolean("learn")
  } else {
    false
  }

  val testDragons = if (learn) {
    val li = List.newBuilder[Dragon]
    for (s <- 0 to 10) {
      for (c <- 0 to 10) {
        for (w <- 0 to 10) {
          for (f <- 0 to 10) {
            if (s + c + w + f == 20) {
              li += Dragon(scaleThickness = s, clawSharpness = c, wingStrength = w, fireBreath = f)
            }
          }
        }
      }
    }
    li.result()
  } else {
    List.empty[Dragon]
  }

  /**
    * Run with parameter '100'  <- number of games to be played, ex 100 times here
    *
    * Undocumented ways of running :
    * '1000  ana'  <- 1000 times with analytical games solver.
    * Default games solver is "self learning" solver, missing 2nd parameter or putting what ever except 'ana' starts default solver
    *
    * '12 xxx PT10M'  <- Games is played 10 min, 3rd indicating duration (parsed as java duration which follows ISO),
    * if 3rd parameter is present, then 1st is ignored
    *
    */

  def main(args: Array[String]): Unit = {

    val newGameURL = config.getString("site.url") + config.getString("site.game")
    val solutionURL = config.getString("site.url") + config.getString("site.solution")
    val weatherURL = config.getString("site.url") + config.getString("site.weather")

    val replacement_game_id = config.getString("game_id")

    logger.trace(s"replacement_game_id=" + replacement_game_id)
    logger.trace(s"game=" + newGameURL)
    logger.trace(s"solution=" + solutionURL)
    logger.trace(s"weather=" + weatherURL)
    logger.trace(s"learn=" + learn)

    val weatherProvider = new Weather

    val numTries = if (args.length > 0) {
      args(0).toInt
    } else {
      1
    }

    val solutionProvider = if (args.length > 1 && "ana" == args(1)) {
      new GameSolutionByAnalytic
    } else {
      new GameSolutionByLearning
    }

    val durationOp = if (args.length > 2) {
      Some(Duration.parse(args(2)))
    } else {
      None
    }

    val gamePlay = new GamePlay(weatherProvider, solutionProvider, replacement_game_id, newGameURL, solutionURL, weatherURL)

    durationOp match {
      case Some(duration) => gamePlay.runDuring(duration)
      case None => gamePlay.runTimes(numTries)
    }


    println(solutionProvider.heilMessage)
    solutionProvider.close

  }

}


final class GamePlay(val weather: WeatherRequest, val solution: GameSolutionProvider,
                     val replacement_game_id: String, val newGameURLstr: String, val solutionURLstr: String, val weatherURLstr: String) extends StrictLogging {


  require(weather != null)
  require(solution != null)
  require(replacement_game_id != null)
  require(newGameURLstr != null)
  require(solutionURLstr != null)
  require(weatherURLstr != null)

  val httpClient = HttpClientBuilder.create().build()
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)


  def runDuring(duration: Duration) = {
    val endTime = Instant.now().plus(duration)
    var counter = 0
    var hits = 0.0
    var hitsPer100 = 0.0

    while (Instant.now().isBefore(endTime)) {
      counter += 1
      val (find: Boolean, hit: Boolean) = play(Source.fromURL(newGameURLstr).mkString)
      if (hit) {
        hits += 1
      }
      hitsPer100 = 100 * hits / counter
      println(f", for $counter  battles, overall success ratio $hitsPer100%1.2f %% ")

    }
    logger.info(f"From $counter knight attacks $hitsPer100%1.2f %%  victoriously defended !")

  }

  def runTimes(times: Int): Unit = {
    if (times < 1) {
      logger.warn("Can not play :-( ")
    } else {
      var hits = 0.0
      var hitsPer100 = 0.0
      for (i <- 1 to times) {
        val (find: Boolean, hit: Boolean) = play(Source.fromURL(newGameURLstr).mkString)
        if (hit) {
          hits += 1
        }
        hitsPer100 = 100 * hits / i
        println(f", overall success ratio $hitsPer100%1.2f %% ")
      }
      logger.info(f"From $times knight attacks $hitsPer100%1.2f %%  victoriously defended !")
    }
  }

  def play: (Boolean, Boolean) = {
    play(Source.fromURL(newGameURLstr).mkString)
  }

  def play(gameJson: String): (Boolean, Boolean) = {
    val (game: Game, weather: WeatherCode, dragonOpt: Option[Dragon]) = findSolution(gameJson)
    dragonOpt match {
      case Some(dragon) =>
        val resB = sendSolution(game, dragon)
        if (!resB) {
          logger.warn(s"$dragon killed (solution REJECTED) for ${game.knight} in $weather ")
          print(s"$dragon killed by ${game.knight} in $weather")
          findGoodDragon(game, weather)
        } else {
          val rs = s"HIT -> good $dragon kills ${game.knight} in $weather"
          logger.info(rs)
          print(rs)
          (true, true)
        }

      case None =>
        if (GamePlay.learn && WeatherStormy != weather) {
          print(s"no good dragon to match ${game.knight} in $weather, learning ...")

          findGoodDragon(game, weather)
        } else {
          print(s"no dragon can match ${game.knight} in $weather")
          logger.warn(s"no solution for ${game.knight} in weather $weather")
          (false, false)
        }
    }

  }


  private def findGoodDragon(game: Game, weather: WeatherCode): (Boolean, Boolean) = {
    GamePlay.testDragons.par.find({
      dragon =>
        if (sendSolution(game, dragon)) {
          solution.addSolution(game.knight, weather, dragon)
          true
        } else {
          logger.trace(s"$dragon is no solution for ${game.knight}")
          false
        }
    })

    if (solution.findDragon(game.knight, weather).isDefined) {
      (true, false)
    } else {
      logger.warn(s"THERE IS NO solution for ${game.knight} in weather $weather")
      (false, false)
    }
  }

  def findSolution(gameJson: String) = {
    val game = objectMapper.readValue(gameJson, classOf[Game])
    val battle_weather = weather.getWeather(new URL(weatherURLstr.replaceAllLiterally(replacement_game_id, game.gameId)))
    logger.trace(s"weather for game $game is $battle_weather")
    val dragon = solution.findDragon(game.knight, battle_weather)
    (game, battle_weather, dragon)
  }

  def sendSolution(game: Game, dragon: Dragon): Boolean = {
    val hput = new HttpPut(solutionURLstr.replaceAllLiterally(replacement_game_id, game.gameId))

    val params = new StringEntity(dragonJson(dragon), "UTF-8")
    params.setContentType("application/json")
    hput.addHeader("content-type", "application/json")
    hput.setEntity(params)
    val response = httpClient.execute(hput)

    if (response.getStatusLine.getStatusCode != 200) {
      sys.error(s"Can not send response to ${solutionURLstr.replaceAllLiterally(replacement_game_id, game.gameId)}")
      System.exit(1)
    }

    val result = objectMapper.readValue(Source.fromInputStream(response.getEntity.getContent).mkString, classOf[GameResult])
    hput.completed()
    if (result.status == "Victory") {
      true
    } else {
      false
    }
  }

  private def dragonJson(dragon: Dragon) =
    s"""
    {
    "dragon": {
      "scaleThickness": ${dragon.scaleThickness},
      "clawSharpness": ${dragon.clawSharpness},
      "wingStrength": ${dragon.wingStrength},
      "fireBreath": ${dragon.fireBreath}
      }
    }"""


}

