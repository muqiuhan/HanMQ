package com.muqiuhan.hanmq.core

import zio.http.WebSocketChannel
import zio.Queue
import zio.Ref
import zio.ZIO
import zio.ZLayer
import zio.Fiber

/** The contract for managing basic mapping relationships of message queue consumers.
 *  This includes adding, retrieving, and removing WebSocket channels associated with specific queues,
 *  as well as removing a channel from all subscribed queues.
 */
trait BasicMap:
  /** Add a WebSocket channel as a consumer to the specified queue.
   *  If the queue does not exist, it will be created.
   *
   *  @param queueName The name of the queue to which the consumer will be added.
   *  @param channel   The WebSocketChannel representing the consumer.
   *  @return A ZIO effect that completes when the consumer is added.
   */
  def addConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit]

  /** Retrieve the set of WebSocket channels (consumers) currently subscribed to the specified queue.
   * 
   *  @param queueName The name of the queue.
   *  @return A ZIO effect that yields a Set of WebSocketChannels subscribed to the queue.
   *          Returns an empty set if the queue does not exist or has no consumers.
   */
  def getConsumers(queueName: String): ZIO[Any, Nothing, Set[WebSocketChannel]]

  /** Remove a specific WebSocket channel from the set of consumers for a given queue.
   *
   *  @param queueName The name of the queue from which the consumer will be removed.
   *  @param channel   The WebSocketChannel to be removed.
   *  @return A ZIO effect that completes when the consumer is removed.
   */
  def removeConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit]

  /** Remove a specific WebSocket channel from all queues it is currently subscribed to.
   *  This is typically used when a client disconnects to ensure proper resource cleanup.
   *
   *  @param channel The WebSocketChannel to be removed from all queues.
   *  @return A ZIO effect that completes when the channel is removed from all queues.
   */
  def removeChannelFromAllQueues(channel: WebSocketChannel): ZIO[Any, Nothing, Unit]
end BasicMap

object BasicMap:
  /** Accessor for the `addConsumer` method in the `BasicMap` service.
   *
   *  @param queueName The name of the queue.
   *  @param channel   The WebSocketChannel.
   *  @return A ZIO effect that requires a `BasicMap` service and adds the consumer.
   */
  inline def addConsumer(queueName: String, channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    ZIO.serviceWithZIO[BasicMap](_.addConsumer(queueName, channel))

  /** Accessor for the `getConsumers` method in the `BasicMap` service.
   *
   *  @param queueName The name of the queue.
   *  @return A ZIO effect that requires a `BasicMap` service and retrieves the consumers.
   */
  inline def getConsumers(queueName: String): ZIO[BasicMap, Nothing, Set[WebSocketChannel]] =
    ZIO.serviceWithZIO[BasicMap](_.getConsumers(queueName))

  /** Accessor for the `removeConsumer` method in the `BasicMap` service.
   *
   *  @param queueName The name of the queue.
   *  @param channel   The WebSocketChannel.
   *  @return A ZIO effect that requires a `BasicMap` service and removes the consumer.
   */
  inline def removeConsumer(queueName: String, channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    ZIO.serviceWithZIO[BasicMap](_.removeConsumer(queueName, channel))

  /** Accessor for the `removeChannelFromAllQueues` method in the `BasicMap` service.
   *
   *  @param channel The WebSocketChannel.
   *  @return A ZIO effect that requires a `BasicMap` service and removes the channel from all queues.
   */
  inline def removeChannelFromAllQueues(channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    ZIO.serviceWithZIO[BasicMap](_.removeChannelFromAllQueues(channel))

  /** Live implementation of the `BasicMap` service.
   *  It manages the mapping of queue names to sets of WebSocket channels (consumers)
   *  using `zio.Ref` for thread-safe state management.
   */
  val live: ZLayer[Any, Nothing, BasicMap] =
    ZLayer.fromZIO(
        // Create a Ref to hold the mutable map of queue names to their consumer sets.
        // Each consumer set is also wrapped in a Ref for granular updates.
        Ref.make(Map.empty[String, Ref[Set[WebSocketChannel]]]).map {
          ref =>
            new BasicMap:
              /** Add a consumer to a queue. If the queue doesn't exist, it's initialized with the new channel. */
              override def addConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit] =
                ref.get.flatMap {
                  map =>
                    map.get(queueName) match
                      // If the queue already exists, update its consumer set.
                      case Some(consumerSetRef) => consumerSetRef.update(_ + channel)
                      // If the queue does not exist, create a new Ref for its consumer set and add it to the main map.
                      case None =>
                        Ref.make(Set(channel)).flatMap {
                          newConsumerSetRef =>
                            ref.update(_ + (queueName -> newConsumerSetRef))
                        }
                }.unit // Ensure the effect returns Unit

              /** Retrieve consumers for a given queue. */
              override inline def getConsumers(queueName: String): ZIO[Any, Nothing, Set[WebSocketChannel]] =
                ref.get.flatMap(
                    _.get(queueName)                     // Get the Ref[Set[WebSocketChannel]] for the queue.
                      .map(_.get)                        // If found, get the Set[WebSocketChannel] from the Ref.
                      .getOrElse(ZIO.succeed(Set.empty)) // If not found, return an empty set.
                )

              /** Remove a specific consumer from a queue. */
              override inline def removeConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit] =
                ref.get.flatMap(
                    _.get(queueName)              // Get the Ref[Set[WebSocketChannel]] for the queue.
                      .map(_.update(_ - channel)) // If found, remove the channel from the set.
                      .getOrElse(ZIO.unit)        // If not found, do nothing.
                )

              /** Remove a channel from all queues it's currently subscribed to. */
              override inline def removeChannelFromAllQueues(channel: WebSocketChannel): ZIO[Any, Nothing, Unit] =
                ref.get.flatMap {
                  map =>
                    // Iterate over all consumer sets and remove the channel.
                    ZIO.foreachDiscard(map.values)(consumerSetRef =>
                      consumerSetRef.update(_ - channel)
                    )
                }
        }
    )
end BasicMap
