package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import models._

object Application extends Controller {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def takePicture = WebSocket.async[JsValue] { request =>
    Party.join
  }
}