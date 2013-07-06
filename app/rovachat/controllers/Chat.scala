package rovachat.controllers

import play.api.mvc.{Action, WebSocket, Controller}
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.iteratee.Iteratee
import play.api.libs.concurrent.Akka
import akka.actor.Props
import rovachat.actors._
import akka.pattern.ask
import play.api.Play.current
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import rovachat.actors.Connected
import rovachat.actors.Join
import rovachat.actors.Broadcast
import rovachat.models.ChatChannel
import rovachat.socket.SocketMember

object Chat extends Controller {

  val chatActor = Akka.system.actorOf(Props[Chat])
  implicit val timeout = Timeout(5 seconds)

  def index = Action { implicit request =>
    Ok(views.html.index("Chat"))
  }

  def live = WebSocket.async[JsValue] { request =>
    chatActor ? Join() map {
      case Connected(enumerator, member) =>
        val iteratee = buildMessageHandler(member)
        (iteratee, enumerator)
    }
  }

  def buildMessageHandler(member: SocketMember) = {
    Iteratee.foreach[JsValue] {
      case obj: JsObject =>
        (obj \ "action").as[String] match {
          case "auth" => chatActor ! Login(member, (obj \ "username").as[String])
          case "broadcast" => chatActor ! Broadcast((obj \ "message").as[String])
          case "channels" =>
            chatActor ? GetChannels(member) map {
              case msg: JsonMessage => member.channel.push(msg.toJson)
            }
          case "channel_send" => chatActor ! SendToChannel(member, (obj \ "message").as[String], ChatChannel((obj \ "channel").as[String]))
          case "channel_join" => chatActor ! JoinChannel(member, (obj \ "name").as[String])
          case "channel_leave" => chatActor ! LeaveChannel(member, (obj \ "name").as[String])
          case "channel_add" => chatActor ! AddChannel((obj \ "name").as[String])
          case "channel_sendimage" => chatActor ! SendImage(member, (obj \ "url").as[String], (obj \ "channel").as[String])
        }
      case _ => println("Invalid response")
    }.mapDone {
      _ => chatActor ! Disconnect(member)
    }
  }
}