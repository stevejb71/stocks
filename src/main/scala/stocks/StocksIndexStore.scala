package stocks

import org.mongodb.scala.MongoClient
import codecs._
import mongo._

class StocksIndexStore(val client: MongoClient) {
  def store(index: String, symbols: List[String]): Unit = {
    for(symbol <- symbols) {
      val stock = Stock(index, symbol)
      client.replaceOne("Indices", stock, stock.primaryKeyFilter)
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
