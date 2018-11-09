package stocks

import java.time.LocalDate

import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}

case class PriceData(open: Double, close: Double, low: Double, high: Double, volume: Int)

case class TimeSeries(symbol: Stock, prices: Map[LocalDate, PriceData])

case class Financials(source: String, stock: Stock, date: LocalDate, dividendYield: Double, beta: Double, marketCap: Double)

case class Stock(index: String, symbol: String)

object codecs {
  implicit val codecRegistry: CodecRegistry = fromRegistries(
    fromProviders(
      classOf[Stock],
      classOf[Financials]
    ),
    DEFAULT_CODEC_REGISTRY)
}
