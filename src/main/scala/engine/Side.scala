package engine

import play.api.libs.json.{JsResult, JsValue, Json}

sealed trait Side

case object Buy extends Side

case object Sell extends Side
