package stocks

import java.io.{File, FileWriter}
import java.time.LocalDate
import java.util.concurrent.Executors

import org.mongodb.scala.MongoClient
import ujson.Js.Value

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.util.Try

object Download extends App {
  val financialDataStorage = new File("/media/steve/80f22ce4-343e-4811-b390-0b6a41449321/stocks-data")

//  downloadAllStockDataFromYahoo()

  val x = parseYahooFinancials()
  new FinancialsStore(MongoClient()).store(x)

  def downloadAllStockDataFromYahoo(): Unit = {
    implicit val threadPool = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(8))
    val allSymbols = ujson.read(Download.getClass.getResourceAsStream("/hkex.json").readAllBytes())

    for (symbolJson <- List("0005.HK")) {
      val symbol = symbolJson//.str
      val symbolFile = new File(financialDataStorage, symbol)
      if (!symbolFile.isFile) {
        Future {
          val symbolDataResponse = requests.get(s"https://finance.yahoo.com/quote/$symbol/")
          println(s"Downloaded $symbol...")
          if (symbolDataResponse.statusCode == 200) {
            val symbolData = symbolDataResponse.text()
            val writer = new FileWriter(symbolFile)
            writer.write(symbolData)
            writer.close()
          } else {
            println(s"failed at $symbol with $symbolDataResponse")
          }
        }
      }
    }
  }

  def parseYahooFinancials() = {
    val data = financialDataStorage.listFiles().toList.map(file => {
      val stock = file.getName
      val text = Source.fromFile(file).mkString
      for {
        jsonElementStr <- substringBetween(text, "root.App.main = ", ";\n}(this));")
        json = ujson.read(jsonElementStr)
        md <- findMarketData(stock, json)
      } yield md
    })
    data.filter(_.isRight).map(rightOrThrow)
  }

  def rightOrThrow[E, A](x: Either[E, A]): A = {
    val Right(r) = x
    r
  }

  def findMarketData(symbol: String, root: Value) = Try {
    val stores = root("context")("dispatcher")("stores")
    val summaryDetail = stores("QuoteSummaryStore")("summaryDetail")
    val dividendYield = summaryDetail("dividendYield")("raw").num
    val marketCap = summaryDetail("marketCap")("raw").num
    val beta = summaryDetail("beta")("raw").num
    Financials("yahoo", Stock("HKEX", symbol), LocalDate.of(2018, 11, 7), dividendYield, beta, marketCap)
  }.toEither.left.map(_.getMessage)

  def downloadTimeSeries(symbol: String): TimeSeries = {
    val apiKey = args(0)
    val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&outputsize=compact&apikey=$apiKey"
    val response = requests.get(url)
    if (response.statusCode != 200) {
      throw new Exception(s"Bad response ${response.statusCode}")
    }
    val jsonTimeSeries = ujson.read(response.data.text)
    println(jsonTimeSeries("Time Series (Daily)"))
    null
  }

  private def substringBetween(s: String, startDelimiter: String, endDelimiter: String): Either[String, String] = {
    val startIndex = s.indexOf(startDelimiter)
    if (startIndex == -1) {
      Left(s"Couldn't find $startDelimiter")
    } else {
      val endIndex = s.indexOf(endDelimiter, startIndex)
      if (endIndex == -1) {
        Left(s"Couldn't find $endDelimiter")
      } else {
        Right(s.substring(startIndex + startDelimiter.length, endIndex))
      }
    }
  }
}
