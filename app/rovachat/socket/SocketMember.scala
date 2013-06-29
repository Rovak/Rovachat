package rovachat.socket

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.{Json, JsValue}

case class SocketMember(uid: String, channel: Channel[JsValue]) {
  var username: String = "Anonymous"

  def toJson = Json.obj(
    "id" -> uid,
    "name" -> username)
}
