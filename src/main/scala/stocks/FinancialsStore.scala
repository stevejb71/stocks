package stocks

import org.mongodb.scala.{FindObservable, MongoClient}
import org.mongodb.scala.model.Sorts.descending
import stocks.codecs._
import stocks.mongo._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class FinancialsStore(client: MongoClient) {
  def store(financials: List[Financials]): Unit = {
    for(financial <- financials) {
      client.replaceOne("Financials", financial, financial.primaryKeyFilter)
    }
  }

  def sortedByMarketCap(): List[Financials] = {
    val sorted: FindObservable[Financials] = client.financialsCollection()
      .find()
      .sort(descending("marketCap"))
    Await.result(sorted.toFuture(), Duration.Inf).toList
  }
}
