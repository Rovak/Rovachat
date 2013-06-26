package rovachat.models

import play.api.libs.json.Json

case class ChatChannel(name: String) {
  def toJson = Json.obj(
    "name" -> name
  )
}
