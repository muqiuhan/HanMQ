package com.muqiuhan.hanmq.legacy.config

import java.util.Properties
import com.muqiuhan.hanmq.legacy.core.QueueManager
import com.muqiuhan.hanmq.legacy.core.WorkerManager
import scala.util.{Try, Success, Failure}

object Config:

  def init() =
    scribe.info("loading config")
    Try(load()) match
      case Failure(e) =>
        scribe.error("Can't not find Config.properties or the format is wrong.")
        throw new RuntimeException("Can't not find Config.properties or the format is wrong.")
      case _ => ()
    end match
  end init

  private def load(): Unit =
    scribe.info("Loading configuration...")
    val config       = new Properties()
    val configStream = getClass().getClassLoader().getResourceAsStream("config.properties")

    if configStream == null then
      scribe.error("Cannot find config.properties in classpath")
      throw new RuntimeException("Cannot find config.properties in classpath")
    else
      config.load(configStream)
      scribe.info(s"Configuration loaded: queueNum=${config.get("queueNum")}, bindingKeys=${config.get(
                "bindingKeys")}, queueNames=${config.get("queueNames")}")

      Try(init(Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
              config.get("bindingKeys").asInstanceOf[String].split(",\\s+"),
              Some(config.get("queueNames").asInstanceOf[String].split(",\\s+")))) match
        case Failure(e) =>
          scribe.warn("Not found queueNames definition, use default naming strategy.");
          init(Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
              config.get("bindingKeys").asInstanceOf[String].split(",\\s+"), None)
        case _ => ()
      end match
    end if
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
