package pt.sergio.scalatokens.domain

import org.joda.time.DateTime

case class AccessTokenConfiguration(tokenId: String, scopes: Set[String], token: Option[AccessToken] = None, validUntil: Option[DateTime] = None)

case class AccessToken(token: String, tokenType: String, expiresIn: Long) { 
  
  def withExpirationTime(expiresInMillisToSet: Long) = this.copy(expiresIn = expiresInMillisToSet)

}

case class AccessTokenRequest(accessTokenConfiguration: AccessTokenConfiguration, forceNewToken: Boolean = false)
case class AccessTokenResponse(token: Either[AccessTokenError, AccessToken])

case class ClientCredentials(id: String, secret: String)
case class UserCredentials(username: String, password: String)

trait AccessTokenError
case object AccessTokenUnauthorizedError extends AccessTokenError
case object AccessTokenParsingError extends AccessTokenError
case class AccessTokenGenericError(errorMessage: String) extends AccessTokenError

