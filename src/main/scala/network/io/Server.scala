package network.io

import lombok.extern.slf4j.Slf4j
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel

@Slf4j(topic = "Server")
class Server():
  private val mainGroup = new NioEventLoopGroup()
  private val subGroup = new NioEventLoopGroup()
  private val server = new ServerBootstrap()
    .group(mainGroup, subGroup)
    .channel(classOf[NioServerSocketChannel])
    .childHandler(new ServerInitializer())

/*
  override def start(): Unit =
    // TODO: Load banner and initialize configuration
    // val channel = server.channel()
    Unit
 */
