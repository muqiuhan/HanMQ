package config

import java.util.Properties
import core.QueueManager
import core.WorkerManager
import com.typesafe.scalalogging.Logger

object Config {
    private val log = Logger(getClass())

    try {
        val config = new Properties()
        config.load(
          getClass().getClassLoader().getResourceAsStream("config.properties")
        )

        try {
            init(
              Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
              config.get("bindingKeys").asInstanceOf[String].split(",\\s+"),
              Some(config.get("queueNames").asInstanceOf[String].split(",\\s+"))
            )
        } catch {
            case e: Exception =>
                log.warn(
                  "Not found queueNames definition, use default naming strategy."
                );

                init(
                  Integer.parseInt(config.get("queueNum").asInstanceOf[String]),
                  config.get("bindingKeys").asInstanceOf[String].split(",\\s+"),
                  None
                )
        }
    } catch {
        case e: Exception =>
            log.error(
              "Can't not find Config.properties or the format is wrong."
            )
            throw new RuntimeException(
              "Can't not find Config.properties or the format is wrong."
            )
    }

    private def init(
        queueNum: Int,
        bindingKeys: Array[String],
        queueNames: Option[Array[String]]
    ): Unit = {
        QueueManager.init(queueNum, bindingKeys, queueNames)
        WorkerManager.init(queueNum)

        if (QueueManager.initialized && WorkerManager.initialized) {
            log.info("EVERYTHING IS READY!!!")
        } else {
            throw new ExceptionInInitializerError()
        }
    }
}
