package ee.dragongame

import java.net.URL

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.{Config, ConfigFactory}
import ee.dragongame.elements.{Dragon, Game, GameResult}
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

import scala.io.Source

object GamePlay {
  val config = ConfigFactory.load(this.getClass.getClassLoader)

  def main(args: Array[String]): Unit = {

    val newGameURL = config.getString("site.url") + config.getString("site.game")
    val solutionURL = config.getString("site.url") + config.getString("site.solution")
    val weatherURL = config.getString("site.url") + config.getString("site.weather")

    val replacement_game_id = config.getString("game_id")

    val solutionProvider = new GameSolution
    val weatherProvider = new Weather

    println("replacement_game_id=" + replacement_game_id)
    println("game=" + newGameURL)
    println("solution=" + solutionURL)
    println("weather=" + weatherURL)

    println(solutionURL.replaceAllLiterally(replacement_game_id, "12345678"))


  }

  /*
  url str for game request
  url str for weather request
  url str for game solution

  get new game
  get wheater report for new game
  get solution for the game
  calculate statistics, log record
  * */


}

class GamePlay(val weather: WeatherRequest, val solution: GameSolutionProvider,
               val replacement_game_id: String, val newGameURLstr: String, val solutionURLstr: String, val weatherURLstr: String) {
  require(weather != null)
  require(solution != null)
  require(replacement_game_id != null)
  require(newGameURLstr != null)
  require(solutionURLstr != null)
  require(weatherURLstr != null)

  val httpClient = HttpClientBuilder.create().build()
  val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)


  def runTimes(times: Int): Unit = {

  }

  def play: Boolean = {
    val gameJson = Source.fromURL(newGameURLstr).mkString
    val game = objectMapper.readValue(gameJson, classOf[Game])
    val battle_weather = weather.getWeather(new URL(weatherURLstr.replaceAllLiterally(replacement_game_id, game.gameId)))

    val dragon = solution.findDragon(game, battle_weather)


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


  final def dragonJson(dragon: Dragon) =
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

