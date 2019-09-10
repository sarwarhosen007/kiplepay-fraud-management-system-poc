package models

case class UserJson(email: String, mobile: String, full_name: String, ip: String, user_agent: String)

case class MerchantJson(name: String, id: String)

case class TransactionJson(amount: Double, currency: String)

case class Input(user: UserJson, merchant: MerchantJson, transaction: TransactionJson)

case class User(id: Int, email: String, mobile: String, full_name: String)

case class Transaction(var id: Int, var email: String, ip: String, user_agent: String, merchant: String, merchant_id: String, amount: Double, currency: String, var status: Boolean, var transaction_type: String, var transaction_datetime: String, var comment: String)

