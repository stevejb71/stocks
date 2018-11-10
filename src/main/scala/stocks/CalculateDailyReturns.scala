package stocks

import java.time.LocalDate

import org.mongodb.scala.MongoClient

import mongo._
import codecs._

case class DailyReturn(date: LocalDate, volume: Long, value: Double)

case class Stats(stock: Stock, meanVolume: Long, mean: Double, stdDev: Double)

class CalculateDailyReturns(client: MongoClient) {
  private implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _

  def dailyReturnsForStock(stock: Stock): Vector[DailyReturn] = {
    val priceData = new PriceDataStore(client).find(stock).filterNot(_.close.abs < 0.001).sortBy(_.date)
    val dayWithNextDay = priceData.zip(priceData.drop(1))
    dayWithNextDay.map { case (d0, d1) =>
      DailyReturn(d0.date, d0.volume, (d1.close - d0.close) / d0.close)
    }
  }

  def stats(stock: Stock, dailyReturns: Vector[DailyReturn]): Stats = {
    val sumReturns = dailyReturns.map(_.value).sum
    val sumSquares = dailyReturns.map(x => x.value * x.value).sum
    val mean = sumReturns / dailyReturns.length
    val variance = sumSquares / dailyReturns.length - mean * mean
    val meanVolume = dailyReturns.map(_.volume).sum / dailyReturns.length
    Stats(stock, meanVolume, mean, Math.sqrt(variance))
  }
}

object CalculateDailyReturnsApp extends App {
  val client = MongoClient()
  val allStocks = new PriceDataStore(client).allStocks()
  val calcDailyReturns = new CalculateDailyReturns(client)
  val stats = allStocks.map(stock => {
    calcDailyReturns.stats(stock, calcDailyReturns.dailyReturnsForStock(stock))
  }).filter(_.meanVolume > 100000).filter(_.mean > 0.0).sortBy(_.stdDev)

  for(stat <- stats) {
    println(stat)
  }


}