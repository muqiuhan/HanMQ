package com.muqiuhan.hanmq.protocol.json

import com.muqiuhan.hanmq.protocol.codec.*
import upickle.default.*
import zio.ZIO

/** JSON 协议消息格式（兼容旧版本） */
case class JsonMessage(
    typ: Int,
    content: String,
    extend: String,
    date: String
) derives ReadWriter

/** JSON 编解码器实现 */
object JsonCodec extends MessageCodec[JsonMessage]:
  override def encode(message: JsonMessage): ZIO[Any, CodecError, String] =
    ZIO.attempt(write(message))
      .mapError(e => CodecError.EncodeError(s"Failed to encode message: ${e.getMessage}", e))

  override def decode(data: String): ZIO[Any, CodecError, JsonMessage] =
    ZIO.attempt(read[JsonMessage](data))
      .mapError(e => CodecError.DecodeError(s"Failed to decode message: ${e.getMessage}", e))
