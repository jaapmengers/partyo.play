package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import models._

object Application extends Controller {

  def index = Action { implicit request =>
    request.cookies.get("facebookId") match{
      case Some(_) => Ok(views.html.index())
      case None => Ok(views.html.select())
    }
  }

  def select(id: String) = Action { implicit request =>
    Redirect(routes.Application.index).withCookies(Cookie("facebookId", id))
  }

  def takePicture = WebSocket.async[JsValue] { request =>
    val facebookId = request.cookies.get("facebookId").get.value
    Party.join(facebookId)
  }
}