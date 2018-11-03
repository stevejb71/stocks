package stocks

import java.time.LocalDate

case class PriceData(open: Double, close: Double, low: Double, high: Double, volume: Int)

case class TimeSeries(symbol: String, prices: Map[LocalDate, PriceData])