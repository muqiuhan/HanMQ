package com.muqiuhan.hanmq.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.stream.ChunkedWriteHandler

class ServerInitializer extends ChannelInitializer[SocketChannel]:
  protected override def initChannel(channel: SocketChannel): Unit =
    val pipeline = channel.pipeline()
    scribe.info(s"Initializing channel for: ${channel.remoteAddress()}")
    pipeline.addLast(new HttpServerCodec())
    pipeline.addLast(new ChunkedWriteHandler())
    pipeline.addLast(new HttpObjectAggregator(1024 * 64))
    pipeline.addLast(new WebSocketServerProtocolHandler("/ws"))
    scribe.info("WebSocket protocol handler added")
    pipeline.addLast(new MessageHandler())
    scribe.info("Message handler added")
  end initChannel
end ServerInitializer
