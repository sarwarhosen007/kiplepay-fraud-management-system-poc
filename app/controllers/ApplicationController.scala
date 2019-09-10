package controllers
import javax.inject.Inject

import play.api.db._
import play.api.mvc._

class ApplicationControllerInject @Inject()(db: Database, val controllerComponents: ControllerComponents)
    extends BaseController {

  def index = Action {
    var outString = "Number is "
    //val conn      = db.getConnection()

    // access "default" database
    db.withConnection { conn =>
      // do whatever you need with the connection
      val rs = conn.prepareStatement("insert into tblUser values (3, 'sarwar@gmail.com', '017777', 'test name', '192.168.0.192', 'user')").execute()

  // connection
  //   .prepareStatement("select * from test where id = 10")
  //   .executeQuery()
    }
    Ok(outString)
  }

}