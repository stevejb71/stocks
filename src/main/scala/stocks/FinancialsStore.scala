package stocks

import org.mongodb.scala.MongoClient
import stocks.codecs._
import stocks.mongo._

class FinancialsStore(client: MongoClient) {
  def store(financials: List[Financials]): Unit = {
    for(financial <- financials) {
      client.replaceOne("Financials", financial, financial.primaryKeyFilter)
    }
  }
}
