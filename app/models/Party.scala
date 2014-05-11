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
import models.PartyRooms._

object PartyRooms {
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
      val newActor = Akka.system.actorOf(Props[PartyActor])
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
          actor ! TakePicture
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

class PartyActor extends Actor {
  val (chatEnumerator, chatChannel) = Concurrent.broadcast[JsValue]

  def receive = {

    case Join => {
        sender ! Connected(chatEnumerator)
      }

    case TakePicture => {
      notifyAll("takePicture")
    }
  }

  def notifyAll(kind: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind)
      )
    )
    chatChannel.push(msg)
  }
}


case object Join
case object Quit
case object TakePicture

case class Connected(enumerator:Enumerator[JsValue])
case class CannotConnect(msg: String)