package rovachat.socket

import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.json.JsValue

case class SocketMember(uid: String, channel: Channel[JsValue]) {

}
