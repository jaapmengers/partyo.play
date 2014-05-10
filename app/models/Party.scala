package models

import akka.actor._
import scala.concurrent.duration._
import scala.language.postfixOps

import play.api._
import play.api.libs.json._
import play.api.libs.iteratee._
import play.api.libs.concurrent._

import akka.util.Timeout
import akka.pattern.ask

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import models.Party._


object Robot {

  def apply(chatRoom: ActorRef) {

    // Create an Iteratee that logs all messages to the console.
    val loggerIteratee = Iteratee.foreach[JsValue](event => Logger("robot").info(event.toString))

    implicit val timeout = Timeout(1 second)
    // Make the robot join the room
    chatRoom ? (Join) map {
      case Connected(robotChannel) =>
        // Apply this Enumerator on the logger.
        robotChannel |>> loggerIteratee
    }

    // Make the robot talk every 30 seconds
    Akka.system.scheduler.schedule(
      30 seconds,
      30 seconds,
      chatRoom,
      Talk("I'm still alive")
    )
  }

}


object Party {
  implicit val timeout = Timeout(1 second)

  var actors:Map[String, ActorRef] = Map()

  def getActor(id: String): ActorRef = {
    val actor = actors.get(id)
    if(actor.isDefined){
      println("Get existing actor: " + id)
      actor.get
    }
    else {
      println("Add new actor: " + id)
      val newActor = Akka.system.actorOf(Props[Party])
      actors += (id -> newActor)
      newActor
    }
  }

  def join(id: String): scala.concurrent.Future[(Iteratee[JsValue, _], Enumerator[JsValue])] = {
    val actor = getActor(id)

    (actor ? Join).map {

      case Connected(enumerator) =>

        // Create an Iteratee to consume the feed
        val iteratee = Iteratee.foreach[JsValue] { event =>
          actor ! Talk((event \ "text").as[String])
        }.map { _ =>
          actor ! Quit
        }

        (iteratee, enumerator)

      case CannotConnect(error) =>

        // Connection error

        // A finished Iteratee sending EOF
        val iteratee = Done[JsValue, Unit]((), Input.EOF)

        // Send an error and close the socket
        val enumerator = Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee, enumerator)

    }

  }
}

class Party extends Actor {
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def receive = {

    case Join => {
        sender ! Connected(chatEnumerator)
        self ! NotifyJoin
      }

    case Talk(msg) =>
      notifyAll("talk", msg)

    case NotifyJoin => {
      notifyAll("join", "test")
    }
  }

  def notifyAll(kind: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind),
        "message" -> JsString(text)
      )
    )
    chatChannel.push(msg)
  }
}


case object Join
case object Quit
case class Talk(message: String)
case object NotifyJoin

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)