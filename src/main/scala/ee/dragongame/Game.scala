package ee.dragongame

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.http.client.methods.HttpPut
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder

import scala.io.Source


/*
 (8,6,4,2) ->
 (3,4,10,3)  -> GameResult(Victory,Dragon was successful in a glorious battle)
 */

case class Knight(agility: Int, armor: Int, attack: Int, endurance: Int, name: String)

case class Game(gameId: Long, knight: Knight)

case class GameResult(status: String, message: String)

object Game {
  def game_base_url(gameid: Long) = s"http://www.dragonsofmugloar.com/api/game/$gameid/solution"

  def dragon(scaleThickness: Int, clawSharpness: Int, wingStrength: Int, fireBreath: Int) =
    s"""
    {
    "dragon": {
      "scaleThickness": $scaleThickness,
      "clawSharpness": $clawSharpness,
      "wingStrength": $wingStrength,
      "fireBreath": $fireBreath
      }
    }"""

  val game1 = 4441503
  val game2 = 8613491

  val gameOneString =
    """  {
                          "gameId": 4441503,
                            "knight": {
                              "agility": 7,
                              "armor": 8,
                              "attack": 3,
                              "endurance": 2,
                              "name": "Sir. Chase Moreno of Manitoba"
                            }
                          }"""
  val gameTwoString =
    """  {
                          "gameId": 8613491,
                            "knight": {
                              "agility": 8,
                              "armor": 6,
                              "attack": 4,
                              "endurance": 2,
                              "name": "Sir. Dale Moore of Nova Scotia"
                            }
                          }"""


  val WEATHER_URL = "http://www.dragonsofmugloar.com/weather/api/report/"
  val WEATHER_2 =
    """<?xml version="1.0" encoding="UTF-8"?><report><time>Mon Sep 05 2016 19:15:04 GMT+0000 (UTC)</time><coords><x>3916.234</x><y>169.914</y><z>6.33</z></coords><code>NMR</code><message>Another day of everyday normal regular weather, business as usual, unless it’s going to be like the time of the Great Paprika Mayonnaise Incident of 2014, that was some pretty nasty stuff.</message><varX-Rating>8</varX-Rating></report> """
  val WEATHER_1 = WEATHER_2

  def main(args: Array[String]): Unit = {
    val objectMapper = new ObjectMapper()
    objectMapper.registerModule(DefaultScalaModule)

    /*val weather1 = Source.fromURL(s"$WEATHER_URL$game1").mkString
    print(weather1)
    val weatherAsXML = scala.xml.XML.loadString(WEATHER_2)
    val wcode = weatherAsXML \ "code"
    println(wcode.text)
    println(Weather.getWeather(weather1))*/

    val httpClient = HttpClientBuilder.create().build()

    var counter =0

    for ( s  <-  0 to 10){
      for ( c  <-  0 to 10){
        for ( w  <-  0 to 10){
          for ( l  <-  0 to 10){
            if(s+ c+ w+ l == 20){
              counter +=1
              //vak//dragon(i, j, k, l)
              val v = (s, c, w, l)

              val drgn = dragon(s, c, w, l)
              val hput = new HttpPut(game_base_url(game2))

              val params = new StringEntity(drgn, "UTF-8")
              params.setContentType("application/json")
              hput.addHeader("content-type", "application/json")
              hput.addHeader("Accept", "*/*")
              hput.setEntity(params)
              val response = httpClient.execute(hput)
              val responseCode = response.getStatusLine.getStatusCode
              if (response.getStatusLine.getStatusCode == 200 || response.getStatusLine.getStatusCode == 204) {

                val rdata = Source.fromInputStream(response.getEntity.getContent).mkString
                //println(rdata)
                val rgm1 = objectMapper.readValue(rdata, classOf[GameResult])

                println(s"$v  -> $rgm1")
                if(rgm1.status == "Defeat"){
                  //println("Defeat !!! ")
                }else{
                  System.exit(0)

                }
              }

              hput.completed()

            }
          }
        }
      }
    }





    //    println(gameOneString)
    //
    //    val gm1 = objectMapper.readValue(gameOneString, classOf[Game])
    //    println(gm1)
    //
    //    val rgm1 = objectMapper.readValue("""{"status":"Defeat","message":"Dragon could not compete with knights armor"}""", classOf[GameResult])
    //    println(rgm1)
  }

  /*
  8613491, dragon(10,0,10,0), {"status":"Defeat","message":"Dragon could not compete with knights armor"}

   */


  /*
  {
    "gameId": 4441503,
    "knight": {
        "agility": 7,
        "armor": 8,
        "attack": 3,
        "endurance": 2,
        "name": "Sir. Chase Moreno of Manitoba"
    }
}
   */

  /*

  {
    "gameId": 8613491,
    "knight": {
        "agility": 8,
        "armor": 6,
        "attack": 4,
        "endurance": 2,
        "name": "Sir. Dale Moore of Nova Scotia"
    }
}

   */

}
