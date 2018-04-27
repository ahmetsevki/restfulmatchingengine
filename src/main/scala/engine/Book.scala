package engine

import play.api.libs.json.Json

case class BookEntry(qty: Int, prc: BigDecimal)
object BookEntry{
  implicit val jsonFormat = Json.format[BookEntry]
}

case class Book(buys: Seq[BookEntry], sells: Seq[BookEntry])
object Book{
  implicit val jsonFormat = Json.format[Book]
}
