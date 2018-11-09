package stocks

import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.ReplaceOptions
import org.mongodb.scala.{MongoClient, MongoCollection}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

object mongo {
  implicit class ClientExtensions(client: MongoClient)(implicit val codecRegistry: CodecRegistry) {
    private val stocksDatabase = client.getDatabase("STOCKS").withCodecRegistry(codecRegistry)

    def findAll[A: ClassTag](collectionName: String): List[A] = {
      val collection: MongoCollection[A] = stocksDatabase.getCollection[A]("Indices")
      Await.result(collection.find().toFuture(), Duration.Inf).toList
    }

    def replaceOne(collectionName: String, thing: AnyRef, filter: Bson): Unit = {
      val collection: MongoCollection[AnyRef] = stocksDatabase.getCollection(collectionName)
      Await.result(collection.replaceOne(filter, thing, ReplaceOptions().upsert(true)).toFuture(), Duration.Inf)
    }
  }
}