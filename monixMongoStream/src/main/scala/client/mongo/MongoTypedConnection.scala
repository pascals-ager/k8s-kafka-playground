package client.mongo

import cats.effect.{ExitCase, Resource, Sync}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.{ChangeStreamObservable, MongoClient, MongoCollection}
import org.mongodb.{scala => mongoDB}
import org.slf4j.Logger

import scala.language.implicitConversions
import scala.reflect.ClassTag

trait MongoTypedConnection[F[_], T] {
  def getMongoCollection(
      dbName: String,
      collectionName: String
  ): F[MongoCollection[T]]

  def watchCollection(
      collection: MongoCollection[T]
  ): F[ChangeStreamObservable[T]]
}

object MongoTypedConnection {
  def create[F[_], T: ClassTag](
      connectionString: String
  )(
      implicit F: Sync[F],
      codecRegistry: CodecRegistry,
      logger: Logger
  ): Resource[F, MongoTypedConnection[F, T]] = {

    val open: F[MongoClient] = F.delay {
      mongoDB.MongoClient(connectionString)
    }

    Resource
      .makeCase(open) {
        case (con, ExitCase.Completed | ExitCase.Canceled) =>
          F.delay {
            logger.info("Application completed/canceled, cleaning up resources")
            con.close()
          }
        case (con, ExitCase.Error(e)) =>
          F.delay {
            logger.info(s"Application Exited with error ${e.getSuppressed}")
            con.close()
          }
      }
      .map { con =>
        /*
         * ToDo: Probably not a good idea to rely on Mongo to do the decoding?
         *  */
        new MongoTypedConnection[F, T] {
          override def getMongoCollection(
              dbName: String,
              collectionName: String
          ): F[MongoCollection[T]] = F.delay {
            con
              .getDatabase(dbName)
              .withCodecRegistry(codecRegistry)
              .getCollection(collectionName)
              .withDocumentClass[T]()
          }

          override def watchCollection(
              collection: MongoCollection[T]
          ): F[ChangeStreamObservable[T]] =
            F.delay {
              collection.watch()
            }
        }
      }
  }
}
