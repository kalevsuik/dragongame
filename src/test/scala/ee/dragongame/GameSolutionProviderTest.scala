package ee.dragongame

import ee.dragongame.elements.{Dragon, Game, Knight}
import org.scalatest.{FunSuite, Matchers, WordSpecLike}

class GameSolutionProviderTest extends WordSpecLike with Matchers {
  "GameSolution" should {
    val gameSP = new GameSolution
    "non existing game return None" in {
      val game =  Knight(10, 10, 10, 10, "Nemo")
      gameSP.findDragon(game, WeatherNormal) shouldBe None
    }
    "existing game return dragon" in {
      val game = Knight(agility = 8, armor = 6, attack = 4, endurance = 2, "Nemo")
      gameSP.findDragon(game, WeatherNormal) shouldBe Some(Dragon(scaleThickness = 4, clawSharpness = 4, wingStrength = 10, fireBreath = 2))
    }
  }
}

/*

(8,6,4,2) ->
 "agility": 8,
 "armor": 6,
 "attack": 4,
 "endurance": 2,

 GameResult(Victory,Dragon was successful in a glorious battle)

 (3,4,10,3), (3,5,10,2), (3,6,10,1), (4,4,10,2), (4,5,10,1), (5,4,10,1)
 "scaleThickness"
 "clawSharpness"
 "wingStrength"
 "fireBreath"
 */