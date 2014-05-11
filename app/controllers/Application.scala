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

  def discard = Action { implicit request =>
    Redirect(routes.Application.index).discardingCookies(DiscardingCookie("isAdmin"), DiscardingCookie("facebookId"))
  }

  def index = Action { implicit request =>

    val cookies = (
      request.cookies.get("facebookId"),
      request.cookies.get("isAdmin")
    )

    cookies match{
      case (Some(_), Some(_)) => Ok(views.html.camera())
      case (Some(_), None) => Ok(views.html.shutterButton())
      case _ => Ok(views.html.select())
    }
  }

  def select(id: String, isAdmin: Boolean) = Action { implicit request =>
    val cookies = (isAdmin match {
      case true => Seq(Cookie("isAdmin", "true"))
      case false => Seq()
    }) ++ Seq(Cookie("facebookId", id))

    Redirect(routes.Application.index).withCookies(cookies: _*)
  }

  def takePicture = WebSocket.async[JsValue] { request =>
    val facebookId = request.cookies.get("facebookId").get.value
    Party.join(facebookId)
  }
}