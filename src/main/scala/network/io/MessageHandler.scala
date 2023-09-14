package network.io

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.util.concurrent.GlobalEventExecutor
import lombok.extern.slf4j.Slf4j

object MessageHandler:
    val clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
