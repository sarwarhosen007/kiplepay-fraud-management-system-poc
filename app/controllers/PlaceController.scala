package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._

import models.Place
import models.Location

class PlaceController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
    
    implicit val locationWrites: Writes[Location] =
        (JsPath \ "lat").write[Double].and((JsPath \ "long").write[Double])(unlift(Location.unapply))

    implicit val placeWrites: Writes[Place] =
        (JsPath \ "name").write[String].and((JsPath \ "location").write[Location])(unlift(Place.unapply))


    implicit val locationReads: Reads[Location] =
        (JsPath \ "lat").read[Double].and((JsPath \ "long").read[Double])(Location.apply _)

    implicit val placeReads: Reads[Place] =
        (JsPath \ "name").read[String].and((JsPath \ "location").read[Location])(Place.apply _)

    def listPlaces = Action {
        val json = Json.toJson(Place.list)
        Ok(json)
    }

    def savePlace = Action(parse.json) { request =>
        val placeResult = request.body.validate[Place]
        placeResult.fold(
            errors => {
                BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
            },
            place => {
                Place.save(place)
                Ok(Json.obj("status" -> "OK", "message" -> ("Place '" + place.name + "' saved.")))
            }
        )
    }
}