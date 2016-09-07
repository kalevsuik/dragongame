package ee.dragongame

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import ee.dragongame.elements.{Dragon, Knight}
import org.mapdb.{DB, DBMaker, Serializer}

trait GameSolutionProvider {
  def findDragon(knight: Knight, weather: WeatherCode): Option[Dragon]

  def addSolution(knight: Knight, weather: WeatherCode, dragon: Dragon)

  def close

  def heilMessage: String

  final def dragon2Array(dragon: Dragon) = {
    Array(dragon.scaleThickness, dragon.clawSharpness, dragon.wingStrength, dragon.fireBreath)
  }

  final def knight2Array(knight: Knight) = {
    Array(knight.attack, knight.armor, knight.agility, knight.endurance)
  }
}

final class GameSolutionByLearning(learn: Boolean = false) extends GameSolutionProvider with StrictLogging {
  val config = ConfigFactory.load(this.getClass.getClassLoader)

  val machine_learning_db = if (config.hasPath("machine_learning_file")) {
    config.getString("machine_learning_file")
  } else {
    "machine_learning/mlearn.db"
  }

  val db: DB = DBMaker
    .fileDB(machine_learning_db)
    .fileMmapEnableIfSupported()
    .fileMmapPreclearDisable()
    .cleanerHackEnable()
    .closeOnJvmShutdown()
    .make()

  val victoriousDragons = db.treeMap("victory")
    .keySerializer(Serializer.STRING)
    .valueSerializer(Serializer.INT_ARRAY)
    .createOrOpen()

  logger.info(s"${victoriousDragons.size()} dragons ready for battle !")

  override def findDragon(knight: Knight, weather: WeatherCode): Option[Dragon] = {
    val key = DBKey(agility = knight.agility, armor = knight.armor, attack = knight.attack, endurance = knight.endurance, weather)
    val ar = victoriousDragons.get(key.toString)
    if (ar == null || ar.length < 4) {
      None
    } else {
      Some(Dragon(scaleThickness = ar(0), clawSharpness = ar(1), wingStrength = ar(2), fireBreath = ar(3)))
    }
  }

  override def addSolution(knight: Knight, weather: WeatherCode, dragon: Dragon): Unit = {
    val key = DBKey(agility = knight.agility, armor = knight.armor, attack = knight.attack, endurance = knight.endurance, weather)
    val ar = victoriousDragons.get(key.toString)
    if (ar == null || ar.length < 4) {
      victoriousDragons.put(key.toString, dragon2Array(dragon))
      logger.info(s"$knight in $weather shall find a matching dragon $dragon")
    } else {
      logger.info(s"There is already matching dragon for $knight in $weather , but $dragon could do as well")
      victoriousDragons.replace(key.toString, dragon2Array(dragon))
    }
  }


  override def close: Unit = {
    logger.info(heilMessage)
    victoriousDragons.close()
    db.close()
  }

  override def heilMessage: String = s"${victoriousDragons.size()} dragons waiting orders !"
}


/**
  * Algo shamelessly copied from
  * https://github.com/ziombo/dragonsofmugloar/blob/master/index.html
  */
final class GameSolutionByAnalytic extends GameSolutionProvider with StrictLogging {
  val inx = 0 :: 1 :: 2 :: 3 :: Nil
  override def findDragon(knight: Knight, weather: WeatherCode): Option[Dragon] = {

    weather match {
      case WeatheLongTry =>
        Some(Dragon(scaleThickness = 5, clawSharpness = 5, wingStrength = 5, fireBreath = 5))
      case WeatherFog =>
        Some(Dragon(scaleThickness = knight.attack, clawSharpness = knight.armor, wingStrength = knight.agility, fireBreath = knight.endurance))
      case WeatherStormy => None
      case WeatherRain =>
        Some(Dragon(scaleThickness = 5, clawSharpness = 10, wingStrength = 5, fireBreath = 0))
      case _ =>
        val knArray = knight2Array(knight)
        val max = knArray.max
        val min = knArray.min
        val maxInd = knArray.indexOf(max)
        val minInd = knArray.indexOf(min)

        val drArray = knArray.clone()
        drArray(maxInd) += 2

        if (max == min) {
          drArray(1) -= 1
          drArray(2) -= 1
        } else {
          val nInx = inx.filter( inx => inx != maxInd && inx != minInd )
          nInx.foreach( indx =>  drArray(indx) -= 1)

        }
        Some(Dragon(scaleThickness = drArray(0), clawSharpness = drArray(1), wingStrength = drArray(2), fireBreath = drArray(3)))
    }
  }

  override def addSolution(knight: Knight, weather: WeatherCode, dragon: Dragon): Unit = {}

  override def close: Unit = {}

  override def heilMessage: String = "Dragon army ready for orders !"
}
