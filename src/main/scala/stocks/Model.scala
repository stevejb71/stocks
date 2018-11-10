package stocks

import java.time.LocalDate

import com.mongodb.client.model.Filters
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}

case class PriceData(stock: Stock, date: LocalDate, open: Double, close: Double, low: Double, high: Double, volume: Long) {
  def primaryKeyFilter = Filters.and(
    Filters.eq("stock", stock),
    Filters.eq("date", date)
  )
}

case class Financials(source: String, stock: Stock, date: LocalDate, dividendYield: Double, beta: Double, marketCap: Double) {
  def primaryKeyFilter = Filters.and(
    Filters.eq("source", source),
    Filters.eq("stock", stock),
    Filters.eq("date", date)
  )
}

case class Stock(index: String, symbol: String) {
  def primaryKeyFilter = Filters.and(
    Filters.eq("index", index),
    Filters.eq("symbol", symbol)
  )
}

object codecs {
  implicit val codecRegistry: CodecRegistry = fromRegistries(
    fromProviders(
      classOf[Stock],
      classOf[Financials],
      classOf[PriceData]
    ),
    DEFAULT_CODEC_REGISTRY)
}
