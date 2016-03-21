package pt.sergio.scalatokens.domain

import spray.httpx.SprayJsonSupport
import spray.json._

trait DefaultJsonFormats extends DefaultJsonProtocol with SprayJsonSupport {
  
  implicit val AccessTokenFormat = jsonFormat(AccessToken, "access_token", "token_type", "expires_in")
  implicit val UserCredentialsFormat = jsonFormat(UserCredentials, "application_username", "application_password")
  implicit val ClientCredentialsFormat = jsonFormat(ClientCredentials, "client_id", "client_secret")
}