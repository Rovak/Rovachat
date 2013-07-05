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

  def addChannel(channel: ChatChannel) = {
    if (!chatChannels.contains(channel)) {
      chatChannels += channel -> List()
      println("Adding " + channel)
    }
  }

  def joinChannel(member: SocketMember, channel: ChatChannel): Unit = {
    if (chatChannels.contains(channel)) {
      chatChannels(channel) ::= member
    } else {
      addChannel(channel)
      joinChannel(member, channel)
    }
    updateChannels(member)
  }

  def SendToChannel(user: SocketMember, msg: String, channel: ChatChannel) = {
    println("asfsa")
    if (chatChannels.contains(channel)) {
      val message = MessageFilter.filter(msg)
      chatChannels(channel).foreach {
        _.channel.push(ChannelMessage(user, message, channel))
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

  def updateChannels(member: SocketMember) = {

    val memberChannels = chatChannels.filter(_._2.contains(member)).map(_._1)

    member.channel.push(Json.obj(
      "action" -> "channels",
      "channels" -> memberChannels.foldLeft(Json.arr()) {
        case (result, current) => result :+ current.toJson
      }))
  }


  def leaveChannel(member: SocketMember, channel: ChatChannel) = {
    if (chatChannels.contains(channel)) {
      chatChannels(channel) = chatChannels(channel).filter(x => x != member)
      updateChannels(member)
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
      SendToChannel(user, msg, channel)

    case GetChannels(member) => {
      sender ! Channels(chatChannels.map(_._1).toList)
      updateChannels(member)
    }

    case Disconnect(member) => {
      chatChannels.foreach { case (channel, users) =>
        chatChannels(channel) = users.filter(_ != member)
      }
      members = members.filter(_.uid != member.uid)
      updateUsers()
    }

    case AddChannel(name: String) => {
      chatChannels += ChatChannel(name) -> members
    }
    case JoinChannel(member, name) => {
      joinChannel(member, ChatChannel(name))
    }
    case LeaveChannel(member, name) => {
      leaveChannel(member, ChatChannel(name))
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

case class GetChannels(member: SocketMember)

case class Channels(channels: List[ChatChannel]) extends JsonMessage {
  def toJson = Json.obj(
    "action" -> "channels",
    "channels" -> channels.foldLeft(Json.arr()) {
      case (result, current) =>
        result :+ current.toJson
    })
}

case class AddChannel(name: String)

case class JoinChannel(member: SocketMember, name: String)

case class LeaveChannel(member: SocketMember, name: String)

case class Message(msg: String) extends JsonMessage {
  def toJson = Json.obj(
    "action" -> "message",
    "msg" -> msg)
}

case class ChannelMessage(user: SocketMember, msg: String, channel: ChatChannel) extends JsonMessage {
  def toJson = Json.obj(
    "action"  -> "channel",
    "channel" -> channel.name,
    "user"    -> user.username,
    "message" -> msg)
}