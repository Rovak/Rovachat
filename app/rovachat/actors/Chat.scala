package rovachat.actors

import rovachat.socket.SocketMember
import akka.actor.Actor
import play.api.libs.json.{Json, JsValue}
import play.api.libs.iteratee.{Enumerator, Concurrent}
import rovachat.models.ChatChannel

class Chat extends Actor {

  implicit def MessageToJson(message: JsonMessage) = message.toJson

  var members = List[SocketMember]()
  var chatChannels = scala.collection.mutable.Map[ChatChannel, List[SocketMember]]()

  chatChannels += ChatChannel("Lobby") -> List()
  chatChannels += ChatChannel("Public") -> List()
  chatChannels += ChatChannel("Developers") -> List()
  chatChannels += ChatChannel("Consultants") -> List()

  def receive = {

    case Join() =>
      val (chatEnumerator, liveChannel) = Concurrent.broadcast[JsValue]
      var member = SocketMember(java.util.UUID.randomUUID().toString, liveChannel)
      members ::= member
      println(s"Member count ${members.length}")
      chatChannels.foreach {
        channel => chatChannels(channel._1) = channel._2 :+ member
      }
      sender ! Connected(chatEnumerator, member)

    case Broadcast(msg) =>
      members.foreach {
        _.channel.push(Message(msg))
      }

    case SendToChannel(msg, channel) =>
      if (chatChannels.contains(channel)) {
        chatChannels(channel).foreach{
          _.channel.push(ChannelMessage(msg, channel))
        }
      }

    case GetChannels() =>
      sender ! Channels(chatChannels.map(_._1).toList)

    case Disconnect(user) =>
      members = members.filter(_.uid != user.uid)
  }
}

trait JsonMessage {
  def toJson: JsValue
}

case class Join()
case class Disconnect(user: SocketMember)
case class Broadcast(msg: String)
case class SendToChannel(msg: String, channel: ChatChannel)
case class Connected(session: Enumerator[JsValue], member: SocketMember)
case class GetChannels()
case class Channels(channels: List[ChatChannel])

case class Message(msg: String) extends JsonMessage {
  def toJson = Json.obj(
    "action" -> "message",
    "msg" -> msg)
}

case class ChannelMessage(msg: String, channel: ChatChannel) extends JsonMessage {
  def toJson = Json.obj(
    "action" -> "channel",
    "channel" -> channel.name,
    "message" -> msg)
}