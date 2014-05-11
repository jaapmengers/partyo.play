package controllers

import play.api._
import play.api.mvc._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.iteratee._
import play.api.libs.json.JsValue

import models._
import play.api.data.Form
import play.api.data.Forms._

case class Party(_id: Option[BSONObjectID], shortId: String, facebookId: String)
case class PartyViewModel(facebookId: String)

object CreateEventForm {
  val form = Form(
    mapping(
      "facebookId" -> nonEmptyText
    )(PartyViewModel.apply)(PartyViewModel.unapply)
  )
}

object PartyBSONFormats {
  implicit object PartyBSONReader extends BSONDocumentReader[Party]{
    def read(doc: BSONDocument): Party =
      Party(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("shortId").get,
        doc.getAs[String]("facebookId").get
      )
  }

  implicit object PartyBSONWriter extends BSONDocumentWriter[Party] {
    def write(party: Party): BSONDocument =
      BSONDocument(
        "_id" -> party._id.getOrElse(BSONObjectID.generate),
        "shortId" -> party.shortId,
        "facebookId" -> party.facebookId
      )
  }
}

object Application extends Controller {

  def connect: BSONCollection = {

    // gets an instance of the driver
    // (creates an actor system)
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))

    // Gets a reference to the database "plugin"
    val db = connection("partyo")

    // Gets a reference to the collection "acoll"
    // By default, you get a BSONCollection.
    val collection = db("partys")
    collection
  }

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
    Ok(views.html.select())
  }

  def event(id: String) = Action.async { implicit request =>
    import PartyBSONFormats._
    val collection = connect
    val cursor = collection.find(BSONDocument("shortId" -> id)).cursor[Party]
    cursor.collect[List]().map { item =>

      val fbId = item.head.facebookId
      request.cookies.get("isAdmin") match {
        case Some(_) => Ok(views.html.camera(id, fbId))
        case None => Ok(views.html.shutterButton(id, fbId))
      }
    }.recover {
      case e =>
        BadRequest("Niet gevonden")
    }
  }

  def select(id: String, isAdmin: Boolean) = Action { implicit request =>
    val cookies = (isAdmin match {
      case true => Seq(Cookie("isAdmin", "true"))
      case false => Seq()
    })

    Redirect(routes.Application.event(id)).withCookies(cookies: _*)
  }

  def createEvent = Action { implicit request =>
    import PartyBSONFormats._
    def onFormError(form: Form[PartyViewModel]) = BadRequest("No facebookId provided")
    def createEvent(party: PartyViewModel) = {

      val collection = connect
      val id = BSONObjectID.generate
      val shortid = id.stringify.substring(0, 6)
      collection.insert(Party(Some(id), shortid, party.facebookId))

      Redirect(routes.Application.event(shortid))
    }

    CreateEventForm.form.bindFromRequest.fold(onFormError, createEvent)
  }

  def takePicture(eventId: String) = WebSocket.async[JsValue] { request =>
    PartyRooms.join(eventId)
  }
}