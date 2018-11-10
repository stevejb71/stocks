package stocks

import java.time.LocalDate

import org.mongodb.scala.MongoClient
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Sorts._
import stocks.codecs._
import stocks.mongo._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PriceDataStore(client: MongoClient) {
  def getLastUpdate(symbol: String): LocalDate = {
    val priceDataCollection = client.priceDataCollection()
    val dateObservable = priceDataCollection.find(Filters.eq("stock.symbol", symbol))
      .sort(descending("date"))
      .first()
    val priceData = Await.result(dateObservable.toFuture(), Duration.Inf)
    if(priceData == null) LocalDate.MIN else priceData.date
  }

  def find(stock: Stock): List[PriceData] = {
    client.findAll[PriceData]("Prices")
  }

  def store(priceDatas: List[PriceData]): Unit = {
    for (priceData <- priceDatas) {
      client.replaceOne("Prices", priceData, priceData.primaryKeyFilter)
    }
  }
}
