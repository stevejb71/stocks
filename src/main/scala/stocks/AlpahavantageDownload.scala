package stocks

import java.time.{DayOfWeek, LocalDate}
import java.util.concurrent.Executors

import org.mongodb.scala.MongoClient

import scala.concurrent.{ExecutionContext, Future}

object AlpahavantageDownload {
  def download(apiKey: String, stock: Stock, full: Boolean): Either[String, List[PriceData]] = {
    val symbol = stock.symbol
    val outputSize = if (full) "full" else "compact"
    val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&outputsize=$outputSize&apikey=$apiKey"
    val response = requests.get(url)
    if (response.statusCode != 200) {
      Left(s"Failed with ${response.statusMessage}")
    } else {
      val value = ujson.read(response.text())
      val timeSeries = value("Time Series (Daily)").obj
      val priceDataList = timeSeries.map { case (dateStr, pricesJson) => {
        val date = LocalDate.parse(dateStr)
        val pricesObj = pricesJson.obj
        PriceData(
          stock,
          date,
          pricesObj("1. open").str.toDouble,
          pricesObj("4. close").str.toDouble,
          pricesObj("3. low").str.toDouble,
          pricesObj("2. high").str.toDouble,
          pricesObj("5. volume").str.toLong
        )
      }
      }
      Right(priceDataList.toList)
    }
  }
}

object AlpahavantageDownloadApp extends App {
  private val lock = new Object()
  implicit val threadPool = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  def toRight[A](x: Either[_, A]): A = {
    val Right(y) = x
    y
  }

  def getPrevBusinessDay(d: LocalDate): LocalDate = {
    if (d.getDayOfWeek == DayOfWeek.SATURDAY) {
      d.minusDays(1)
    } else if (d.getDayOfWeek == DayOfWeek.SUNDAY) {
      d.minusDays(2)
    } else {
      d
    }
  }

  val apiKey = args(0)
  val mongo = MongoClient()
  val pricesDataStore = new PriceDataStore(mongo)
  val allSymbols = new StocksIndexStore(mongo).read().map(_.symbol)
  val limitDay = getPrevBusinessDay(LocalDate.now())
  var timeTaken = 0L
  var numDownloads = 0
  var numSkipped = 0
  val symbolsByMarketCap = new FinancialsStore(mongo).sortedByMarketCap().drop(50).take(50).map(_.stock.symbol)
  val force = true
  for (symbol <- List("2800.HK", "3126.HK", "3084.HK", "3101.HK", "3110.HK", "3085.HK", "1299.HK")) {
    val lastDay = pricesDataStore.getLastUpdate(symbol)
    if (force || lastDay.isBefore(limitDay)) {
      Future {
        try {
          println(s"Downloading full data for $symbol")
          val startTime = System.currentTimeMillis()
          val downloaded: Either[String, List[PriceData]] = AlpahavantageDownload.download(apiKey, Stock("HKEX", symbol), full = true)
          println(s"Download for $symbol done")
          if (downloaded.isRight) {
            val priceData = toRight(downloaded)
            pricesDataStore.store(priceData)
            println(s"Storing $symbol in Mongo done")
            synchronized {
              numDownloads += 1
              timeTaken += System.currentTimeMillis() - startTime
              val averageTimeForDownload = timeTaken.toDouble / numDownloads / 1000.0
              val remainingDownloads = allSymbols.length - numDownloads - numSkipped
              val remainingTime = (remainingDownloads * averageTimeForDownload).toInt
              println(s"Average time per download and store $averageTimeForDownload, remaining downloads $remainingDownloads, will take $remainingTime seconds")
            }
          } else {
            println(s"Downloading failure for $symbol")
          }
        } catch {
          case e: Throwable => {
            println(s"Failure for symbol $symbol: ${e.getMessage}")
          }
        }
      }
      Thread.sleep(2000)
    } else {
      println(s"$symbol is up to date")
      numSkipped += 1
    }
  }
}