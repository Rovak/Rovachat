package rovachat.controllers

import play.api.mvc.{Action, WebSocket, Controller}
import play.api.libs.json.{Json, JsObject, JsValue}
import play.api.libs.iteratee.{Iteratee, Concurrent}
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

object Chat extends Controller {

  val chatActor = Akka.system.actorOf(Props[Chat])
  implicit val timeout = Timeout(5 seconds)

  val (chatEnumerator, liveChannel) = Concurrent.broadcast[JsValue]

  def index = Action {
    Ok(views.html.index("Chat"))
  }

  def live = WebSocket.async[JsValue] { request =>
    chatActor ? Join() map {
      case Connected(enumerator, member) =>
        val iteratee = Iteratee.foreach[JsValue] {
          case obj: JsObject =>
            (obj \ "action").as[String] match {
              case "auth" =>
                chatActor ! Login(member, (obj \ "username").as[String])
              case "broadcast" =>
                chatActor ! Broadcast((obj \ "message").as[String])
              case "channels" =>
                chatActor ? GetChannels() map {
                  case Channels(channels) =>
                    member.channel.push(Json.obj(
                      "action" -> "channels",
                      "channels" -> channels.foldLeft(Json.arr()) {
                        case (result, current) =>
                          result :+ current.toJson
                      })
                    )
                }
              case "channel" =>
                chatActor ? SendToChannel(member, (obj \ "message").as[String], ChatChannel((obj \ "channel").as[String]))
              case "addchannel" =>
                chatActor ? AddChannel((obj \ "name").as[String])

            }
        }.mapDone {
          _ =>
            chatActor ! Disconnect(member)
            println("disconnected")
        }

        (iteratee, enumerator)
    }
  }
}
