package utils

import com.typesafe.scalalogging.Logger
import scala.io.Source

object Banner {
    private val log = Logger(getClass)

    private val banner = """"""

    def load(): Unit = {
        System.getProperty("os.name") match {
            case "Linux" | "Macos" => printLinux
            case "Windows"         => printWindows
        }
    }

    def printLinux: Unit = {
        println(
          Source
              .fromFile(
                getClass.getClassLoader().getResource("banner-linux").getPath()
              )
              .getLines
              .mkString("\u001b[38;5;10m", "\n", "\u001b[0m")
        )
    }

    def printWindows: Unit = {
        println(
          Source
              .fromFile(
                getClass.getClassLoader().getResource("banner-linux").getPath()
              )
              .getLines
              .mkString("\n")
        )
    }
}
