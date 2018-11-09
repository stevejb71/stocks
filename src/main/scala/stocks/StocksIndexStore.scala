package stocks

import org.mongodb.scala.MongoClient
import org.mongodb.scala.model.Filters
import stocks.codecs._
import stocks.mongo._

class StocksIndexStore(val client: MongoClient) {
  def store(index: String, symbols: List[String]): Unit = {
    for(symbol <- symbols) {
      client.replaceOne("Indices", Stock(index, symbol),
        Filters.and(
          Filters.eq("index", index),
          Filters.eq("symbol", index)
        )
      )
    }
  }

  def read(): List[Stock] = client.findAll[Stock]("Indices")
}

object FromFileSystem extends App {
  val hkexSymbols = ujson.read(Download.getClass.getResourceAsStream("/hkex.json").readAllBytes()).arr.toList.map(_.str)
  private val stocks = new StocksIndexStore(MongoClient())
  stocks.store("HKEX", hkexSymbols)
  println(stocks.read())
}
