package config

import java.util.Properties
import core.QueueManager
import core.WorkerManager

object Config:

  def apply() =
    try
      val config = new Properties()
      config.load(getClass().getClassLoader().getResourceAsStream("config.properties"))

      try
        init(Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
            config.get("bindingKeys").asInstanceOf[String].split(",\\s+"),
            Some(config.get("queueNames").asInstanceOf[String].split(",\\s+")))
      catch
        case e: Exception =>
          scribe.warn("Not found queueNames definition, use default naming strategy.");

          init(Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
              config.get("bindingKeys").asInstanceOf[String].split(",\\s+"), None)
      end try
    catch
      case e: Exception =>
        scribe.error("Can't not find Config.properties or the format is wrong.")
        throw new RuntimeException("Can't not find Config.properties or the format is wrong.")
    end try
  end apply

  private def init(queueNum: Int, bindingKeys: Array[String], queueNames: Option[Array[String]]): Unit =
    QueueManager.init(queueNum, bindingKeys, queueNames)
    WorkerManager.init(queueNum)

    if QueueManager.initialized && WorkerManager.initialized then scribe.info("EVERYTHING IS READY!!!")
    else throw new ExceptionInInitializerError()
  end init
end Config
