package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._

import models.Input
import models.UserJson
import models.MerchantJson
import models.TransactionJson
// Spark
import spark.InputJob

class InputController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
    
    implicit val userReads: Reads[UserJson] = (
            (JsPath \ "email").read[String] and
            (JsPath \ "mobile").read[String] and
            (JsPath \ "full_name").read[String] and
            (JsPath \ "ip").read[String] and
            (JsPath \ "user_agent").read[String]
        )(UserJson.apply _)

    implicit val merchantReads: Reads[MerchantJson] = (
            (JsPath \ "name").read[String] and
            (JsPath \ "id").read[String]
        )(MerchantJson.apply _)

    implicit val transactionReads: Reads[TransactionJson] = (
            (JsPath \ "amount").read[Double] and
            (JsPath \ "currency").read[String]
        )(TransactionJson.apply _)

    implicit val inputReads: Reads[Input] = (
            (JsPath \ "user").read[UserJson] and
            (JsPath \ "merchant").read[MerchantJson] and
            (JsPath \ "transaction").read[TransactionJson]
        )(Input.apply _)

    def readInput = Action(parse.json).async{ request =>
        val inputResult = request.body.validate[Input]
            inputResult.fold(
                errors => {
                    Future{BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))}
                },
                input => {
                    

                    val status = Future{InputJob.Validation(input)}
                    status.map{ s => Ok(Json.obj("status" -> {s}))}
                }
            )
        // val futureSum = Future{InputJob.Save}
        // futureSum.map{ s => Ok(views.html.test_args(s"A non-blocking call to Spark with result: ${s + 1000}"))}
    }
}