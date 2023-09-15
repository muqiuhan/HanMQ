package network.io

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.stream.ChunkedWriteHandler

class ServerInitializer extends ChannelInitializer[SocketChannel] {
    protected override def initChannel(channel: SocketChannel): Unit = {
        val pipeline = channel.pipeline()
        pipeline.addLast(new HttpServerCodec())
        pipeline.addLast(new ChunkedWriteHandler())
        pipeline.addLast(new HttpObjectAggregator(1024 * 64))
        pipeline.addLast(new WebSocketServerProtocolHandler("/"))
        // pipeline.addLast(new MessageHandler())

    }
}
