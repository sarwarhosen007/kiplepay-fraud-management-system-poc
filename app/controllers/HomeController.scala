package controllers

import javax.inject.Inject
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.json.Reads._
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits._
import play.api.db._
import play.api.data.Forms._
import play.api.data._
import models.{BlackListEmail,BlackListIp}

class HomeController @Inject()(db: Database,cc: ControllerComponents) extends AbstractController(cc) {

   val addEmailForm = Form(
    mapping(
      "email" -> nonEmptyText
    )(BlackListEmail.apply)(BlackListEmail.unapply)
  )

  val addIpForm = Form(
    mapping(
      "IP" -> nonEmptyText
    )(BlackListIp.apply)(BlackListIp.unapply)
  )

  def home = Action{ implicit request =>
    // access "default" database
    var emailList : Array[String] = Array()
    var ipList : Array[String] = Array()

    db.withConnection { conn =>

      // retrive blackListEmail
      val emails = conn.prepareStatement("select * from tblBlackListEmail").executeQuery()
      while (emails.next()) {
        emailList +:= emails.getString("email")
         
      }
    
      // retrive blackListIp
      val ips = conn.prepareStatement("select * from tblBlackListIp").executeQuery()
      while (ips.next()) {
        ipList +:= ips.getString("IP")
        println(ips.getString("IP"))
      }
    }
    Ok(views.html.home(addEmailForm,addIpForm,emailList,ipList))
  }


  def addBlackListEmail = Action(parse.form(addEmailForm)) { request =>
      val emailData = request.body
      val newEmail = new BlackListEmail(email = emailData.email.trim)
    // access "default" database
    db.withConnection { conn =>
      // do whatever you need with the connection
      val rs = conn.prepareStatement("insert into tblBlackListEmail(email) values ('"+emailData.email+"')").execute()
      }
      
      Redirect(routes.HomeController.home())
  }

  def addBlackListIp = Action(parse.form(addIpForm)) { request =>
      val ipData = request.body
      val newIp = new BlackListIp(IP = ipData.IP.trim)
    // access "default" database
    db.withConnection { conn =>
      // do whatever you need with the connection
      val rs = conn.prepareStatement("insert into tblBlackListIP(IP) values ('"+ipData.IP+"')").execute()
      }
     
      Redirect(routes.HomeController.home())
  }

  // def createWidget = Action(parse.form(Application.createWidgetForm)) { request =>
  //   val widget = request.body
  //   widgets.append(widget)
  //   Redirect(routes.Application.listWidgets)
  // }

/*
  def loginPost = {
    Action.async(parse.form(addEmailForm)) { implicit request =>
      val emailData = request.body
      val newEmail = new BlackListEmail(email = addEmailForm.email.trim)
      print(newEmail.email)
    }
  }

*/

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  // A simple example to call Apache Spark
  // def test : Action[AnyContent] = Action { implicit request =>
  // 	val sum = SparkTest.Example
  //   Ok(views.html.test_args(s"A call to Spark, with result: $sum"))
  // }

  // def mymethod : Action[AnyContent] = Action { implicit request =>
  // 	val status = SparkTest.TestBool(id)
  //   Ok(Json.toJson(A))
  // }

  // A non-blocking call to Apache Spark 
  // def testAsync = Action.async{
  // 	val futureSum = Future{SparkTest.Example}
  //   futureSum.map{ s => Ok(views.html.test_args(s"A non-blocking call to Spark with result: ${s + 1000}"))}
  // }

}
