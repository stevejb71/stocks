package stocks

object Download extends App {
  val stocks = List("0005.HK", "0006.HK")
  val apiKey = args(0)

  println(downloadStockData("0005.HK"))


  def downloadStockData(symbol: String): String = {
    val url = s"https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=$symbol&outputsize=compact&apikey=$apiKey"
    val response = requests.get(url)
    if(response.statusCode != 200) {
      throw new Exception(s"Bad response ${response.statusCode}")
    }
    response.data.text
  }
}
