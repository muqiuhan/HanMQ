package utils

import scala.Console.*
import scala.io.Source

object Banner:

  private val banner = """
          :::    :::        :::        ::::    :::      :::   :::      :::::::: 
         :+:    :+:      :+: :+:      :+:+:   :+:     :+:+: :+:+:    :+:    :+: 
        +:+    +:+     +:+   +:+     :+:+:+  +:+    +:+ +:+:+ +:+   +:+    +:+  
       +#++:++#++    +#++:++#++:    +#+ +:+ +#+    +#+  +:+  +#+   +#+    +:+   
      +#+    +#+    +#+     +#+    +#+  +#+#+#    +#+       +#+   +#+    +#+    
     #+#    #+#    #+#     #+#    #+#   #+#+#    #+#       #+#   #+#    #+#     
    ###    ###    ###     ###    ###    ####    ###       ###    ###########

        o- Github : https://github.com/muqiuhan/HanMQ
        o- Version: 0.1.0
        o- Copyright (c) 2023 - 2024 Muqiu Han
    """

  def load(): Unit =
    println()

    System.getProperty("os.name") match
      case "Linux" | "Macos" => println(banner.split("\n").mkString("\u001b[38;5;10m", "\n", "\u001b[0m"))
      case "Windows"         => println(banner.split("\n").mkString("\n"))
    end match
  end load
end Banner
