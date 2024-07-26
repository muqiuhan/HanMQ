package config

import java.util.Properties
import core.QueueManager
import core.WorkerManager
import scala.util.{Try, Success, Failure}

object Config:

  def apply() =
    Try(load()) match
      case Failure(e) =>
        scribe.error("Can't not find Config.properties or the format is wrong.")
        throw new RuntimeException("Can't not find Config.properties or the format is wrong.")
      case _ => ()
  end apply

  private def load(): Unit =
    val config = new Properties()
    config.load(getClass().getClassLoader().getResourceAsStream("config.properties"))

    Try(init(Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
            config.get("bindingKeys").asInstanceOf[String].split(",\\s+"),
            Some(config.get("queueNames").asInstanceOf[String].split(",\\s+")))) match
      case Failure(e) =>
        scribe.warn("Not found queueNames definition, use default naming strategy.");
        init(Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
            config.get("bindingKeys").asInstanceOf[String].split(",\\s+"), None)
      case _ => ()
    end match
  end load

  private def init(queueNum: Int, bindingKeys: Array[String], queueNames: Option[Array[String]]): Unit =
    QueueManager.init(queueNum, bindingKeys, queueNames)
    WorkerManager.init(queueNum)

    if QueueManager.initialized && WorkerManager.initialized then
      scribe.info("Everything is ready")
    else
      throw ExceptionInInitializerError()
    end if
  end init
end Config
