package stocks

import java.io.{File, FileWriter}

import ujson.Js.Value

import scala.util.{Random, Try}

object Download extends App {
  val random = new Random()
  val stocks = List("0005.HK", "0006.HK")
  val apiKey = args(0)
  val allSymbols = ujson.read(Download.getClass.getResourceAsStream("/hkex.json").readAllBytes())
  val storage = new File("/media/steve/80f22ce4-343e-4811-b390-0b6a41449321/stocks-data")

  for(symbolJson <- allSymbols.arr) {
    val symbol = symbolJson.str
    val symbolFile = new File(storage, symbol)
    if(!symbolFile.isFile) {
      println(s"Downloading $symbol...")
      val symbolDataResponse = requests.get(s"https://finance.yahoo.com/quote/$symbol/")
      if(symbolDataResponse.statusCode == 200) {
        val symbolData = symbolDataResponse.text()
        val writer = new FileWriter(symbolFile)
        writer.write(symbolData)
        writer.close()
      } else {
        println(s"failed at $symbol with $symbolDataResponse")
      }
      Thread.sleep(2000 + random.nextInt(1500))
    }
  }

  val hk0001 = new String(Download.getClass.getResourceAsStream("0001.html").readAllBytes())
  //    requests.get("https://finance.yahoo.com/quote/0001.HK/")
  val jsonElementStr = substringBetween(hk0001, "root.App.main = ", ";\n}(this));")

  val financials = jsonElementStr.map(x => findMarketData(ujson.read(x)))
  println(financials)


  def findMarketData(root: Value) = Try {
    val stores = root("context")("dispatcher")("stores")
    val summaryDetail = stores("QuoteSummaryStore")("summaryDetail")
    val dividendYield = summaryDetail("dividendYield")("raw").num
    val marketCap = summaryDetail("marketCap")("raw").num
    val beta = summaryDetail("beta")("raw").num
    Financials(dividendYield, beta, marketCap)
  }.toEither

  def downloadTimeSeries(symbol: String): TimeSeries = {
    val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&outputsize=compact&apikey=$apiKey"
    val response = requests.get(url)
    if (response.statusCode != 200) {
      throw new Exception(s"Bad response ${response.statusCode}")
    }
    val jsonTimeSeries = ujson.read(response.data.text)
    println(jsonTimeSeries("Time Series (Daily)"))
    null
  }

  private def substringBetween(s: String, startDelimiter: String, endDelimiter: String): Option[String] = {
    val startIndex = s.indexOf(startDelimiter)
    if (startIndex == -1) {
      None
    } else {
      val endIndex = s.indexOf(endDelimiter, startIndex)
      if (endIndex == -1) {
        None
      } else {
        Some(s.substring(startIndex + startDelimiter.length, endIndex))
      }
    }
  }
}
