package pt.sergio.scalatokens

import java.net.URLEncoder
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.util.Timeout
import pt.sergio.scalatokens.domain._
import spray.client.pipelining._
import spray.http.HttpCharsets.`UTF-8`
import spray.http.HttpHeaders.Authorization
import spray.http.StatusCodes._
import spray.http.Uri.Query
import spray.http._
import spray.httpx.unmarshalling._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.io.Source

trait AccessTokenSupport extends ConfigSupport with DefaultJsonFormats {

  implicit def actorSystem: ActorSystem
  implicit val timeout = Timeout(loadedConfig.getDuration("oauth.ask-timeout", TimeUnit.SECONDS).seconds)

  /** Reads the user credentials from the application properties file and converts from JSON to [[UserCredentials]]*/
  def getUserCredentials: UserCredentials = Source.fromFile(userFilePath).getLines().mkString.parseJson.convertTo[UserCredentials]
  
  /** Reads the client credentials from the application properties file and converts from JSON to [[ClientCredentials]]*/
  def getClientCredentials: ClientCredentials = Source.fromFile(clientFilePath).getLines().mkString.parseJson.convertTo[ClientCredentials]

  /** Requests a new token to authentication server with given [[AccessTokenConfiguration]]
    *
    * Constructs the URL to call with all the information needed and requests it from the authentication server defined. 
    */
  def requestNewAccessToken(accessTokenConfiguration: AccessTokenConfiguration): Future[Either[AccessTokenError, AccessToken]] = {

    val userCredentials: UserCredentials = getUserCredentials
    val clientCredentials: ClientCredentials = getClientCredentials
    
    val username = URLEncoder.encode(userCredentials.username, defaultEncoding)
    val password = URLEncoder.encode(userCredentials.password, defaultEncoding)
    val scopes = formatScopesForRequestUrl(accessTokenConfiguration.tokenId, accessTokenConfiguration.scopes)
    val data = s"grant_type=$grantType&scope=$scopes&username=$username&password=$password"

    val pipeline: HttpRequest => Future[HttpResponse] = (
      addHeader(Authorization(BasicHttpCredentials(clientCredentials.id, clientCredentials.secret)))
      ~> sendAndReceive
    )

    val request: HttpRequest = Post(Uri(oauthUrl).copy(query = Query("realm" -> realm)), HttpEntity(ContentType(MediaTypes.`application/x-www-form-urlencoded`, `UTF-8`), data))
    pipeline(request).map {
      response => handleResponse(accessTokenConfiguration, response)
    }
  }

  /** Transforms the access token configuration in the expected URL request format. 
    * For example: ...&scope=tokenXYZ.read+tokenXYZ.write&...
    */
  private def formatScopesForRequestUrl(tokenId: String, scopes: Set[String]): String = {
    var formattedScopes = tokenId
    if (!formattedScopes.isEmpty) {
      formattedScopes += s".${scopes.mkString(s"+$tokenId.")}"
    }
    formattedScopes
  }
  
  /** Returns the access token [[AccessToken]] to the actor asking for it.
    *
    * Evaluates the [[HttpResponse]] provided by the authentication server and attempts to extract the [[AccessToken]]
    * from it. Will throw an exception if something didn't go according to plan. 
    *
    * @param accessTokenConfiguration The access token configuration
    * @param response The response from the authentication server to process
    *                 
    * @return The [[AccessToken]] if the answer contains one. An exception otherwise.
    */
  private def handleResponse(accessTokenConfiguration: AccessTokenConfiguration, response: HttpResponse): Either[AccessTokenError, AccessToken] = {
    response.status match {
      case OK =>
        response.entity.as[AccessToken] match {
          case Left(_) => Left(AccessTokenParsingError)
          case Right(accessToken) => Right(accessToken)
        }
      case Unauthorized => Left(AccessTokenUnauthorizedError)
      case _ => Left(AccessTokenGenericError(s"Unable to get an access token. Error was: [${response.message}]."))
    }
  }

  def sendAndReceive = sendReceive
}