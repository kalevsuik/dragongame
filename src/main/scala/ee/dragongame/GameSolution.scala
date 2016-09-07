package ee.dragongame

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import ee.dragongame.elements.{Dragon, Knight}
import org.mapdb.{DB, DBMaker, Serializer}

trait GameSolutionProvider {
  def findDragon(knight: Knight, weather: WeatherCode): Option[Dragon]
  def addSolution(knight: Knight, weather: WeatherCode, dragon: Dragon)
}

final class GameSolution(learn: Boolean = false) extends GameSolutionProvider with StrictLogging{
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

  def findDragon(knight: Knight, weather: WeatherCode): Option[Dragon] = {
    val key=DBKey(agility = knight.agility,armor = knight.armor,attack = knight.attack,endurance = knight.endurance,weather)
    val ar=victoriousDragons.get(key.toString)
    if(ar==null || ar.length < 4){
      None
    }else{
      Some(Dragon(scaleThickness=ar(0), clawSharpness=ar(1), wingStrength=ar(2), fireBreath=ar(3)))
    }
  }

  def addSolution(knight: Knight, weather: WeatherCode, dragon: Dragon): Unit ={
    val key=DBKey(agility = knight.agility,armor = knight.armor,attack = knight.attack,endurance = knight.endurance,weather)
    val ar=victoriousDragons.get(key.toString)
    if(ar==null || ar.length < 4){
      victoriousDragons.put(key.toString,dragon2Arrray(dragon))
      logger.info(s"$knight in $weather shall find a matching dragon $dragon")
    }else{
      logger.info(s"There is already matching dragon for $knight in $weather , but $dragon could do as well")
    }
  }

  private def dragon2Arrray(dragon: Dragon) ={
    Array(dragon.scaleThickness,dragon.clawSharpness,dragon.wingStrength,dragon.fireBreath)
  }

}
