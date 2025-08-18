package com.muqiuhan.hanmq.core

import zio.Queue
import zio.Ref
import zio.ZIO
import zio.ZLayer
import zio.Fiber
import com.muqiuhan.hanmq.legacy.utils.KeyUtils // Import KeyUtils

/** The contract for managing message queues and their associated worker fibers.
 *  This service is responsible for adding, retrieving, putting messages into, signaling,
 *  and removing queues. It also manages the lifecycle of worker fibers for each queue.
 */
trait QueueManager:
  /** Add a new message queue with a specified name and binding key.
   *  If the queue already exists, the operation will be a no-op.
   *  A dedicated worker fiber is started for each new queue to process messages.
   *
   *  @param queueName  The unique name of the message queue.
   *  @param bindingKey The binding key associated with the queue for routing messages.
   *  @return A ZIO effect that completes when the queue is added and its worker fiber is started.
   */
  def addQueue(queueName: String, bindingKey: String): ZIO[Any, Nothing, Unit]

  /** Retrieve a message queue by its name.
   *
   *  @param queueName The name of the queue to retrieve.
   *  @return A ZIO effect that yields an `Option` containing the `Queue[String]` if found,
   *          otherwise `None`.
   */
  def getQueue(queueName: String): ZIO[Any, Nothing, Option[Queue[String]]]

  /** Put a message into the appropriate message queue(s) based on the routing key.
   *  Messages are offered to queues whose binding keys match the provided routing key.
   *
   *  @param message    The message content to be put into the queue.
   *  @param routingKey The routing key used to determine which queues receive the message.
   *  @return A ZIO effect that completes when the message is offered to matching queues.
   */
  def put(message: String, routingKey: String): ZIO[Any, Nothing, Unit]

  /** Signal a specific queue, primarily to ensure its worker fiber is running.
   *  While `zio.Queue.take` is non-blocking (suspends the fiber), this can be used
   *  to log warnings if a worker fiber is unexpectedly not found for a signaled queue.
   *
   *  @param queueName The name of the queue to signal.
   *  @return A ZIO effect that completes.
   */
  def signal(queueName: String): ZIO[Any, Nothing, Unit]

  /** Remove a message queue and interrupts its associated worker fiber.
   *  This ensures proper cleanup of resources when a queue is no longer needed.
   *
   *  @param queueName The name of the queue to remove.
   *  @return A ZIO effect that completes when the queue and its worker fiber are removed.
   */
  def removeQueue(queueName: String): ZIO[Any, Nothing, Unit]
end QueueManager

object QueueManager:
  /** Accessor for the `addQueue` method in the `QueueManager` service.
   * 
   *  @param queueName  The unique name of the message queue.
   *  @param bindingKey The binding key.
   *  @return A ZIO effect that requires a `QueueManager` service and adds the queue.
   */
  inline def addQueue(queueName: String, bindingKey: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.addQueue(queueName, bindingKey))

  /** Accessor for the `getQueue` method in the `QueueManager` service.
   * 
   *  @param queueName The name of the queue.
   *  @return A ZIO effect that requires a `QueueManager` service and retrieves the queue.
   */
  inline def getQueue(queueName: String): ZIO[QueueManager, Nothing, Option[Queue[String]]] =
    ZIO.serviceWithZIO[QueueManager](_.getQueue(queueName))

  /** Accessor for the `put` method in the `QueueManager` service.
   * 
   *  @param message    The message content.
   *  @param routingKey The routing key.
   *  @return A ZIO effect that requires a `QueueManager` service and puts the message.
   */
  inline def put(message: String, routingKey: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.put(message, routingKey))

  /** Accessor for the `signal` method in the `QueueManager` service.
   * 
   *  @param queueName The name of the queue.
   *  @return A ZIO effect that requires a `QueueManager` service and signals the queue.
   */
  inline def signal(queueName: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.signal(queueName))

  /** Accessor for the `removeQueue` method in the `QueueManager` service.
   * 
   *  @param queueName The name of the queue.
   *  @return A ZIO effect that requires a `QueueManager` service and removes the queue.
   */
  inline def removeQueue(queueName: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.removeQueue(queueName))

  /** Live implementation of the `QueueManager` service.
   *  It manages message queues using `zio.Ref` for thread-safe access to queues and their
   *  associated worker fibers. Worker fibers are responsible for taking messages from queues.
   */
  val live: ZLayer[Any, Nothing, QueueManager] =
    ZLayer.fromZIO {
      for
        // Ref to hold the map of queue names to their ZIO Queues.
        queueRefs <- Ref.make(Map.empty[String, Queue[String]])
        // Ref to hold the map of queue names to their worker Fiber references.
        workerFibersRef <- Ref.make(Map.empty[String, Fiber.Runtime[_, _]])
        _               <- ZIO.succeed(scribe.info("QueueManager initialized"))
      yield new QueueManager:
        /** Add a queue and starts a dedicated worker fiber for it.
         *  The worker fiber runs indefinitely, taking messages from its associated queue.
         */
        override def addQueue(queueName: String, bindingKey: String): ZIO[Any, Nothing, Unit] =
          queueRefs.get.flatMap { map =>
            map.get(queueName) match
              case Some(_) => ZIO.unit // Queue already exists, do nothing.
              case None =>
                for
                  newQueue <- Queue.unbounded[String]                       // Create an unbounded ZIO Queue.
                  _        <- queueRefs.update(_ + (queueName -> newQueue)) // Add the new queue to the map.
                  _        <- ZIO.succeed(scribe.info(s"Queue ${queueName} added"))
                  // Start a daemon worker fiber. It will automatically terminate when the JVM exits.
                  // This fiber continuously takes messages from `newQueue`.
                  _ <- (
                      for
                        _       <- ZIO.succeed(scribe.info(s"Worker for queue ${queueName} started"))
                        message <- newQueue.take.forever // Continuously take messages from the queue.
                        _       <- ZIO.succeed(scribe.debug(s"Worker for queue ${queueName} took message: $message"))
                      // Note: The actual dispatching of messages to consumers is handled elsewhere (e.g., in Server.scala or a separate Dispatcher service)
                      // This worker's primary role is to ensure messages are taken from the queue.
                      yield ()
                  ).forkDaemon.flatMap(fiber => workerFibersRef.update(_ + (queueName -> fiber))) // Store the fiber reference.
                yield ()
          }.unit

        /** Retrieve an existing message queue by its name. */
        override def getQueue(queueName: String): ZIO[Any, Nothing, Option[Queue[String]]] =
          queueRefs.get.map(_.get(queueName))

        /** Put a message into all queues that match the given routing key.
         *  It iterates through all managed queues and checks if their binding key matches the routing key.
         */
        override def put(message: String, routingKey: String): ZIO[Any, Nothing, Unit] =
          queueRefs.get.flatMap {
            map =>
              ZIO.foreachDiscard(map) { case (queueName, queue) =>
                // Use KeyUtils to compare routing key with the queue's binding key (assumed to be queueName for simplicity here).
                if KeyUtils.routingKeyCompare(routingKey, queueName) then
                  queue.offer(message).unit // Offer the message to the matching queue.
                else
                  ZIO.unit
              }
          }

        /** Check if a worker fiber is associated with a given queue.
         *  Logs a warning if no worker is found, indicating a potential configuration or race condition issue.
         */
        override def signal(queueName: String): ZIO[Any, Nothing, Unit] =
          workerFibersRef.get.flatMap {
            fibers =>
              fibers.get(queueName) match
                case Some(fiber) => ZIO.unit // Worker fiber is found and running.
                case None =>
                  ZIO.succeed(scribe.warn(
                          s"Signal received for queue ${queueName} but no worker fiber found. This might indicate an issue."))
          }

        /** Remove a queue and interrupts its corresponding worker fiber.
         *  This ensures that the fiber stops processing messages and related resources are cleaned up.
         */
        override def removeQueue(queueName: String): ZIO[Any, Nothing, Unit] =
          for
            fibers <- workerFibersRef.get
            // Interrupt the worker fiber if it exists.
            _ <- fibers.get(queueName) match
              case Some(fiber) => fiber.interrupt.unit
              case None        => ZIO.unit
            _ <- workerFibersRef.update(_ - queueName) // Remove the fiber reference.
            _ <- queueRefs.update(_ - queueName)       // Remove the queue reference.
            _ <- ZIO.succeed(scribe.info(s"Queue ${queueName} and its worker fiber removed"))
          yield ()
    }
end QueueManager
