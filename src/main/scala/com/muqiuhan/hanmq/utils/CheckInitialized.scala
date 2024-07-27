package com.muqiuhan.hanmq.utils

class CheckInitialized:
  private var _initialized = false

  protected def checkInitialized(): Unit =
    if !_initialized then
      scribe.error("The QueueManager is not initialized")
      throw RuntimeException("The QueueManager is not initialized")
  end checkInitialized

  protected inline def initialize(): Unit =
    _initialized = true

  def initialized: Boolean = _initialized
end CheckInitialized
