package ee.dragongame.elements

case class Dragon(scaleThickness: Int, clawSharpness: Int, wingStrength: Int, fireBreath: Int)
case class Knight(agility: Int, armor: Int, attack: Int, endurance: Int, name: String)

case class Game(gameId: String, knight: Knight)

case class GameResult(status: String, message: String)