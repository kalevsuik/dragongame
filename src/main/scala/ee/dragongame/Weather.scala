package ee.dragongame

import java.net.URL

import scala.xml.Elem

trait WeatherRequest{
  def getWeather (weatherURL: URL): WeatherCode
}


sealed trait WeatherCode

case object WeatherNormal extends WeatherCode

case object WeatherStormy extends WeatherCode

case object WeatherRain extends WeatherCode

case object WeatherFog extends WeatherCode


class Weather extends WeatherRequest{

  def getWeather(weatherURL: URL) = {
    val weatherAsXML = scala.xml.XML.load(weatherURL)

    parseWeatherString(weatherAsXML)

  }

  def parseWeatherString(weatherAsXML: Elem): WeatherCode with Product with Serializable = {
    (weatherAsXML \ "code").text match {
      case "NMR" => WeatherNormal
      case "HVA" => WeatherRain
      case "SRO" => WeatherStormy
      case "FUNDEFINEDG" => WeatherFog
      case "T E" => WeatherNormal
      case s => println(weatherAsXML)
        System.exit(1)
        WeatherFog
    }
  }
}
