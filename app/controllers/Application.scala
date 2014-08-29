package controllers

import play.api._
import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import models._

object Application extends Controller {


  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Application.takePicture
      )
    ).as("text/javascript")
  }

  def index = Action { implicit request =>
    Ok(views.html.shutterButton())
  }

  def camera = Action { implicit request =>
    Ok(views.html.camera())
  }

  def takePicture = WebSocket.async[JsValue] { request =>
    Party.join("default")
  }
}