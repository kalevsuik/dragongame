package ee.dragongame


sealed trait WeatherCode

case object WeatherNormal extends WeatherCode
case object WeatherStormy extends WeatherCode
case object WeatherRain extends WeatherCode
case object WeatherFog extends WeatherCode


object Weather {

  def getWeather(weather:String)={
    val weatherAsXML = scala.xml.XML.loadString(weather)

    (weatherAsXML \ "code").text  match {
      case "NMR" => Some(WeatherNormal)
      case "STORM" => Some(WeatherStormy)
      case "RAIN" => Some(WeatherRain)
      case "FOG" => Some(WeatherFog)
      case _ => None
    }

  }

}
