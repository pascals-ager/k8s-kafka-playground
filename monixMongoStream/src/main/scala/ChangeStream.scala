import cats.effect.{ExitCode, Resource}
import cats.implicits._
import client.mongo.Implicits._
import client.mongo.MongoTypedConnection
import model.Orders
import model.Orders._
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import monix.reactive.Observable
import org.bson.codecs.configuration.CodecConfigurationException
import org.mongodb.scala.{ChangeStreamObservable, MongoTimeoutException}
import org.mongodb.scala.model.changestream.ChangeStreamDocument
import org.slf4j.{Logger, LoggerFactory}

object ChangeStream extends TaskApp {

  def run(args: List[String]): Task[ExitCode] = {
    implicit val scheduler: Scheduler = monix.execution.Scheduler.global
    implicit val logger: Logger       = LoggerFactory.getLogger("monix")

    /*
     * ToDo: Use Config objects
     * */
    val typedConnection: Resource[Task, MongoTypedConnection[Task, Orders]] =
      MongoTypedConnection.create[Task, Orders](
        "mongodb://test:test@192.168.64.2:32017/?authSource=test&authMechanism=SCRAM-SHA-1"
      )

    typedConnection.use { mongoClient =>
      val changeStream: Task[ChangeStreamObservable[Orders]] =
        for {
          collection <- mongoClient.getMongoCollection("test", "orders")
          changes    <- mongoClient.watchCollection(collection)
        } yield changes

      val ordersObservable: Observable[ChangeStreamDocument[Orders]] = for {
        obs    <- Observable.fromTask(changeStream)
        orders <- Observable.fromReactivePublisher(observableToPublisher(obs))
      } yield orders

      ordersObservable
        .attempt
        .collect {
          case Right(order) => order
        }
        .onErrorHandle {
          case timeout: MongoTimeoutException =>
            logger.error(timeout.getMessage)
          case illegal: java.lang.IllegalArgumentException =>
            logger.error(illegal.getMessage)
          case unauthorized: com.mongodb.MongoCommandException =>
            logger.error(unauthorized.getMessage)
          case codecException: CodecConfigurationException =>
            logger.warn(codecException.getMessage)
          /* ToDo: Two reasons I currently see this:
           *   1. Codecs are missing for a type (eg: OffsetDateTime). This should be caught compile time.
           *      Although, it isn't.
           *   2. ChangeStreamDocument is invalid, in which case the error handling should be left to the application.
           *      However, it seems `org.bson.codecs.pojo.PojoCodecImpl.decodePropertyModel` throws a
           *      `CodecConfigurationException` which is not wrapped by OrdersObservable.attempt?
           *       For example, below message is not caught here:
           *       org.bson.codecs.configuration.CodecConfigurationException: Failed to decode 'ChangeStreamDocument'.
           *       Decoding 'fullDocument' errored with: Missing field: "origin"
           *       at org.bson.codecs.pojo.PojoCodecImpl.decodePropertyModel(PojoCodecImpl.java:224)
           *       at org.bson.codecs.pojo.PojoCodecImpl.decodeProperties(PojoCodecImpl.java:197)
           *       at org.bson.codecs.pojo.PojoCodecImpl.decode(PojoCodecImpl.java:121)
           *       at org.bson.codecs.pojo.PojoCodecImpl.decode(PojoCodecImpl.java:125)
           * */
          /* ToDo: Bug: https://jira.mongodb.org/browse/JAVA-3644
           *  Resolved in Mongo Java Driver 3.12.3 and above. Mongo Scala Driver 2.9.0 which is also the latest version
           *  is dependent on Java Driver 3.12.2. This needs to be upgraded.
           *
           *  Alternate option is to not rely on Mongo to code/decode Documents, and handle that in the application.
           * */
          case otherException: Throwable =>
            logger.error("Some other shit went down! Worry later")
            throw otherException
        }
        .foreachL(println) // ideally consumeWith Kafka/GCS
        .as(ExitCode.Success)

    }

  }

}
