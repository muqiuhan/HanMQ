package server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import com.typesafe.scalalogging.Logger
import server.ServerInitializer

/// Netty server main class, responsible for initializing nio thread groups and binding initializers
object Server:
  private val mainGroup = new NioEventLoopGroup()
  private val subGroup  = new NioEventLoopGroup()
  private val server = new ServerBootstrap().group(mainGroup, subGroup).channel(classOf[NioServerSocketChannel])
    .childHandler(new ServerInitializer())

  private val log = Logger(getClass)

  def start(): Unit =
    try
      utils.Banner.load()
      Class.forName("config.Config")
      val channel = server.bind(9993).sync().channel()
      log.info("Server start successfully!")
      channel.closeFuture().sync()
    catch
      case e: Exception =>
        log.error(s"Server error: ${e.getMessage()}")
        e.printStackTrace()
    finally
      mainGroup.shutdownGracefully()
      subGroup.shutdownGracefully()
end Server
