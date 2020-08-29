package model

import org.bson.codecs.configuration.CodecRegistries.{
  fromProviders,
  fromRegistries
}
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.bson.codecs.Macros._

case class Orders(
    id: String,
    version: String,
    createdAt: String,
    lastModifiedAt: String, // ToDo: Change this to OffsetDateTIme and explore writing a codec for it.
    orderNumber: String,
    customerId: String,
    country: String,
    orderState: String,
    InventoryMode: String,
    origin: String
)

object Orders {
  implicit val codecRegistry: CodecRegistry =
    fromRegistries(fromProviders(classOf[Orders]), DEFAULT_CODEC_REGISTRY)
}
