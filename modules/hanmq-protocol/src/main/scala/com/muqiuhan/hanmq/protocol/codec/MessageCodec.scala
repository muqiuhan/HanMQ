package com.muqiuhan.hanmq.protocol.codec

import zio.ZIO

/** 消息编解码器接口 */
trait MessageCodec[A]:
  /** 编码消息 */
  def encode(message: A): ZIO[Any, CodecError, String]

  /** 解码消息 */
  def decode(data: String): ZIO[Any, CodecError, A]

/** 编解码错误 */
sealed trait CodecError extends Exception
object CodecError:
  case class DecodeError(message: String, cause: Throwable) extends CodecError
  case class EncodeError(message: String, cause: Throwable) extends CodecError
