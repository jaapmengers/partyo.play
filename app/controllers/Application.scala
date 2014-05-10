package controllers

import play.api._
import play.api.mvc._
import reactivemongo.bson._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.api._
import scala.concurrent.ExecutionContext.Implicits.global

case class Party(_id: Option[BSONObjectID], facebookId: String)

object Party {
  implicit object PartyBSONReader extends BSONDocumentReader[Party]{
    def read(doc: BSONDocument): Party =
      Party(
        doc.getAs[BSONObjectID]("_id"),
        doc.getAs[String]("facebookId").get
      )
  }

  implicit object PartyBSONWriter extends BSONDocumentWriter[Party] {
    def write(party: Party): BSONDocument =
      BSONDocument(
        "_id" -> party._id.getOrElse(BSONObjectID.generate),
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

  def index = Action.async { implicit request =>

    import Party._

    val collection = connect

    val query = BSONDocument("$query" -> BSONDocument())
    val items = collection.find(query).cursor[Party]

    items.collect[List]().map { partys =>
      Ok(views.html.index(partys))
    }.recover {
      case e =>
        BadRequest(e.getMessage)
    }
  }

}