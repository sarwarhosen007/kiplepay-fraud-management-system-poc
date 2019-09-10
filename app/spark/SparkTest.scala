package spark

import org.apache.spark.SparkConf
import play.api.db._
import org.apache.spark.sql.{Dataset,DataFrame, SparkSession,SaveMode}
import models.Input
import java.util.Properties
import java.time.{Instant, ZoneId, ZonedDateTime} 
import models.User
import models.Transaction

object InputJob {
    val sparkS = SparkSession.builder.master("local[4]").getOrCreate();
    import sparkS.implicits._
    var sql = ""

    val conn = "jdbc:sqlite:database/FraudManagementDB"
    
    val properties = new Properties()
    Class.forName("org.sqlite.JDBC")

  def Validation(input: Input):Boolean = {
    var user = User(0, input.user.email, input.user.mobile, input.user.full_name)
    var transaction = Transaction(0, input.user.email, input.user.ip, input.user.user_agent, input.merchant.name, 
    input.merchant.id, input.transaction.amount, input.transaction.currency, true, "", "", "")

    var doTransaction = true
    //check if total amount transaction in this <=1500. If yes status=false and type = risk high and add comment
    
    println("Amount Validity: " + isValidAmountTrans(user.email, transaction.amount))
    if( isValidAmountTrans(user.email, transaction.amount) ){
        doTransaction= doTransaction && true;
        if(doTransaction)
          transaction.transaction_type= "PROCEED";
    }
    else{
        doTransaction= doTransaction && false;
        transaction.transaction_type= "WARNING";
        transaction.comment = "|| Reached Threshold Transaction Amount This Day (1,500 RM) || "
    }

    //check if total transaction in this day <=3. If yes status=false and type = risk high and add comment
    println("Count Validity: " + isValidCountTrans(user.email))
    if( isValidCountTrans(user.email) ){
        doTransaction= doTransaction && true;
        if(doTransaction)
          transaction.transaction_type= "PROCEED";
    }
    else{
        doTransaction= doTransaction && false;
        transaction.transaction_type= "WARNING";
        transaction.comment += "|| Reached Threshold Transaction Number This Day (3) || "
    }

    //check if in blacklist email. If yes status=false and type = risk high and add comment
    println("Black Listed Email: " + isEmailBlackListed(user.email))
    if( !isEmailBlackListed(user.email) ){
        doTransaction= doTransaction && true;
        if(doTransaction)
          transaction.transaction_type= "PROCEED";
    }
    else{
        doTransaction= doTransaction && false;
        transaction.transaction_type= "BLOCKED";
        transaction.comment = "|| High Risk Email || "
    }
    //check if in blacklist ip. If yes status=false and type = risk high and add comment
    println("Black Listed Ip: " + isIpBlackListed(user.email))
    if( !isIpBlackListed(transaction.ip) ){
        doTransaction= doTransaction && true;
        if(doTransaction)
          transaction.transaction_type= "PROCEED";
    }
    else{
        doTransaction= doTransaction && false;
        transaction.transaction_type= "BLOCKED";
        transaction.comment = "|| High Risk Ip Address || "
    }
    println("Transaction Possible: " + doTransaction)
    
      transaction.status = doTransaction;
      transaction.transaction_datetime = System.currentTimeMillis().toString;
      // check user already exist or not
      sql="select Email from tblUser where email = '"+input.user.email+"'"
      val jdbcDF = sparkS.read
      .format("jdbc")
      .option("url", s"$conn")
      .option("driver", "org.sqlite.JDBC")
      .option("dbtable", s"( $sql ) t")
      .load().createOrReplaceTempView("mytable");
      val dataFrame=sparkS.sql("Select count(*) count from mytable")

      if(dataFrame.head().getLong(0)==0){
        //save user info
        val dfInputUser = Seq((user.email, user.mobile, user.full_name)).toDF("email", "mobile", "full_name")
        dfInputUser.write.mode(SaveMode.Append).jdbc(conn, "tblUser",properties)
      }

      val userId = getUserId(input.user.email);

      // save merchantinfo
      val dfInputTransaction = Seq((transaction.email, transaction.ip, transaction.user_agent, transaction.merchant, 
      transaction.merchant_id, transaction.amount, transaction.currency, transaction.status, transaction.transaction_type, 
      transaction.transaction_datetime, transaction.comment)).toDF("email", "ip", "user_agent", "merchant", 
      "merchant_id", "amount", "currency", "status", "transaction_type", "transaction_datetime", "comment")
      dfInputTransaction.write.mode(SaveMode.Append).jdbc(conn, "tblTransaction",properties)

      return doTransaction;
     
  }

  def getUserId(email: String): Int = {
    sql="select Id from tblUser where email = '"+email+"'"

    val jdbcDF = sparkS.read
    .format("jdbc")
    .option("url", s"$conn")
    .option("driver", "org.sqlite.JDBC")
    .option("dbtable", s"( $sql ) t")
    .load().createOrReplaceTempView("mytable");
    val dataFrame=sparkS.sql("Select Id from mytable");
    
    return dataFrame.head().getLong(0).toInt;
  }

  def isEmailBlackListed(email: String): Boolean = {
    sql="select Id from tblBlackListEmail where email = '"+email+"'"

    val jdbcDF = sparkS.read
    .format("jdbc")
    .option("url", s"$conn")
    .option("driver", "org.sqlite.JDBC")
    .option("dbtable", s"( $sql ) t")
    .load().createOrReplaceTempView("mytable");
    val dataFrame=sparkS.sql("Select count(*) count from mytable")
    
    if(dataFrame.head().getLong(0)==0) false
    else true

  }

  def isIpBlackListed(ip: String): Boolean = {
    sql="select Id from tblBlackListIp where IP = '"+ip+"'"

    val jdbcDF = sparkS.read
    .format("jdbc")
    .option("url", s"$conn")
    .option("driver", "org.sqlite.JDBC")
    .option("dbtable", s"( $sql ) t")
    .load().createOrReplaceTempView("mytable");
    val dataFrame=sparkS.sql("Select count(*) count from mytable")
    
    if(dataFrame.head().getLong(0)==0) false
    else true

  }

  def isValidCountTrans(email: String): Boolean = {
    sql="select Id from tblTransaction where email = '"+email+"' and status = 1 and date(datetime(transaction_datetime / 1000 , 'unixepoch')) = date('now')"

    val jdbcDF = sparkS.read
    .format("jdbc")
    .option("url", s"$conn")
    .option("driver", "org.sqlite.JDBC")
    .option("dbtable", s"( $sql ) t")
    .load().createOrReplaceTempView("mytable");
    val dataFrame=sparkS.sql("Select ifnull(count(*), 0) count from mytable")
    //println("Total Count: "+dataFrame.head().getLong(0)+" nunber")
    if(dataFrame.head().getLong(0)<=3) true
    else false
  }

  def isValidAmountTrans(email: String, amount: Double): Boolean = {
    sql="select amount from tblTransaction where email = '"+email+"' and status = 1 and date(datetime(transaction_datetime / 1000 , 'unixepoch')) = date('now')"

    val jdbcDF = sparkS.read
    .format("jdbc")
    .option("url", s"$conn")
    .option("driver", "org.sqlite.JDBC")
    .option("dbtable", s"( $sql ) t")
    .load().createOrReplaceTempView("mytable");
    val dataFrame=sparkS.sql("Select ifnull(sum(*), 0) from mytable")
    // var d = dataFrame.head().getDouble(0)
    // println(d)
    // if(dataFrame.head(0).isEmpty) true
    // else 
    if(dataFrame.head().getDouble(0)+amount<1500.0) true
    else false
  }

}