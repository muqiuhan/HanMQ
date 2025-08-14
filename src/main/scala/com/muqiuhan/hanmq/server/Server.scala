package com.muqiuhan.hanmq.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import scala.util.Try
import scala.util.Failure
import scala.util.Success

/** Netty server main class, responsible for initializing nio thread groups and binding initializers */
object Server:
  private val mainGroup = new NioEventLoopGroup()
  private val subGroup  = new NioEventLoopGroup()

  private val server =
    new ServerBootstrap()
      .group(mainGroup, subGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ServerInitializer())

  private def init: Channel =
    com.muqiuhan.hanmq.utils.Banner.load()

    scribe.info("load config")
    com.muqiuhan.hanmq.config.Config.init()

    scribe.info("start server")
    scribe.info("Binding to port 1221...")
    val channel = server.bind(1221).sync().channel()
    scribe.info(s"Server started successfully on port 1221, channel: ${channel}")
    channel
  end init

  private def shutdownGracefully: Unit =
    scribe.info("shutdown server")
    mainGroup.shutdownGracefully()
    subGroup.shutdownGracefully()
  end shutdownGracefully

  private implicit class CleanupThrowable(e: Throwable):
    inline def printStackTraceWithLogAndCleanup(): Unit =
      shutdownGracefully
      scribe.error(s"server error: ${e}")
      e.printStackTrace()
    end printStackTraceWithLogAndCleanup
  end CleanupThrowable

  private implicit class CleanupChannelFeature(channelFuture: ChannelFuture):
    inline def cleanup: Unit = shutdownGracefully

  def start(): Unit =
    Try(init) match
      case Failure(e)       => e.printStackTraceWithLogAndCleanup()
      case Success(channel) => channel.closeFuture().sync().cleanup
    end match
  end start

end Server
