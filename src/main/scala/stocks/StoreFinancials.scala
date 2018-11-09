package stocks

import java.io.File

import org.mongodb.scala.{MongoClient, MongoCollection}

object StoreFinancials extends App {
  def store(financials: Seq[Financials]): Unit = {
    val client = MongoClient()
    val financialsDB = client.getDatabase("STOCKS")
    val financialsCollection: MongoCollection[Financials] = financialsDB.getCollection("Financials")
    financialsCollection.insertMany(financials)
  }
}
