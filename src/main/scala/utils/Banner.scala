package utils

import com.typesafe.scalalogging.Logger
import scala.Console._
import scala.io.Source

object Banner {
    private val log = Logger(getClass)

    private val banner = """
          :::    :::        :::        ::::    :::      :::   :::      :::::::: 
         :+:    :+:      :+: :+:      :+:+:   :+:     :+:+: :+:+:    :+:    :+: 
        +:+    +:+     +:+   +:+     :+:+:+  +:+    +:+ +:+:+ +:+   +:+    +:+  
       +#++:++#++    +#++:++#++:    +#+ +:+ +#+    +#+  +:+  +#+   +#+    +:+   
      +#+    +#+    +#+     +#+    +#+  +#+#+#    +#+       +#+   +#+    +#+    
     #+#    #+#    #+#     #+#    #+#   #+#+#    #+#       #+#   #+#    #+#     
    ###    ###    ###     ###    ###    ####    ###       ###    ###########

        o- Github : https://github.com/muqiuhan/HanMQ
        o- Version: 0.0.1
        o- Copyright (c) 2022 Muqiu Han
    """

    def load(): Unit = {
        println()

        System.getProperty("os.name") match {
            case "Linux" | "Macos" =>
                println(
                  banner
                      .split("\n")
                      .mkString("\u001b[38;5;10m", "\n", "\u001b[0m")
                )

            case "Windows" =>
                println(
                  banner.split("\n").mkString("\n")
                )
        }

    }
}
