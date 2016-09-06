package ee.dragongame

import ee.dragongame.elements.{Dragon, Game}

trait GameSolutionProvider{
  def findDragon(game: Game, weather: WeatherCode): Dragon
}
class GameSolution extends GameSolutionProvider{
  def findDragon(game: Game, weather: WeatherCode): Dragon = {
    Dragon(10, 10, 0, 0)
  }

}
