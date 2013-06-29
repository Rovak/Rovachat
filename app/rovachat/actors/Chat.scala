package rovachat.actors

import rovachat.socket.SocketMember
import akka.actor.Actor
import play.api.libs.json.{Json, JsValue}
import play.api.libs.iteratee.{Enumerator, Concurrent}
import rovachat.models.ChatChannel
import rovachat.services.MessageFilter

class Chat extends Actor {

  implicit def MessageToJson(message: JsonMessage) = message.toJson

  var members = List[SocketMember]()
  var chatChannels = scala.collection.mutable.Map[ChatChannel, List[SocketMember]]()

  chatChannels += ChatChannel("Lobby") -> List()
  chatChannels += ChatChannel("Public") -> List()
  chatChannels += ChatChannel("Developers") -> List()
  chatChannels += ChatChannel("Consultants") -> List()

  def channels = chatChannels.map(_._1).toList

  def SendToChannel(msg: String, channel: ChatChannel) = {
    if (chatChannels.contains(channel)) {
      val message = MessageFilter.filter(msg)
      chatChannels(channel).foreach {
        _.channel.push(ChannelMessage(message, channel))
      }
    }
  }

  def updateUsers() = {
    members.foreach { member =>
      member.channel.push(Json.obj(
        "action" -> "users",
        "users" -> members.foldLeft(Json.arr()) {
          case (list, user) => list :+ user.toJson
        }))

    }
  }

  def updateChannels() = {
    members.foreach { member =>
      member.channel.push(Json.obj(
        "action" -> "channels",
        "channels" -> channels.foldLeft(Json.arr()) {
          case (result, current) => result :+ current.toJson
        }))
    }
  }



  def receive = {

    case Join() => {
      val (chatEnumerator, liveChannel) = Concurrent.broadcast[JsValue]
      var member = SocketMember(
        java.util.UUID.randomUUID().toString,
        liveChannel)
      members ::= member
      chatChannels.foreach {
        channel => chatChannels(channel._1) = channel._2 :+ member
      }
      sender ! Connected(chatEnumerator, member)
    }

    case Login(member, username) => {
      member.username = username
      updateUsers()
    }

    case SendToChannel(user, msg, channel) =>
      SendToChannel(s"${user.username}: $msg", channel)

    case GetChannels() => {
      sender ! Channels(chatChannels.map(_._1).toList)
    }

    case Disconnect(user) => {
      members = members.filter(_.uid != user.uid)
      updateUsers()
    }

    case AddChannel(name: String) => {
      chatChannels += ChatChannel(name) -> members
      updateChannels()
    }
  }
}

trait JsonMessage {
  def toJson: JsValue
}

case class Join()

case class Login(member: SocketMember, username: String)

case class Disconnect(user: SocketMember)

case class Broadcast(msg: String)

case class SendToChannel(user: SocketMember, msg: String, channel: ChatChannel)

case class Connected(session: Enumerator[JsValue], member: SocketMember)

case class GetChannels()

case class Channels(channels: List[ChatChannel])

case class AddChannel(name: String)

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