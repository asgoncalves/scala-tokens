package pt.sergio.scalatokens

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import org.joda.time.DateTime
import org.scalatest.{MustMatchers, WordSpecLike}
import pt.sergio.scalatokens.domain._
import spray.http.HttpCharsets._
import spray.http._

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}
import scala.language.postfixOps

class AccessTokenManagerSpec extends TestKit(ActorSystem("test-actor-system")) with MustMatchers with WordSpecLike {

  lazy val default_waiting_time = 2 seconds
  implicit lazy val timeout = Timeout(default_waiting_time)
  implicit val actorSystem = ActorSystem("test-actor-system")
  val config: Config = ConfigFactory.load

  // test constants
  lazy val testToken = "test-token"
  lazy val testTokenType = "BearerTest"
  lazy val testTokenScope = "test-token.all"
  lazy val testSecondsUntilExpires = 10L
  lazy val expirationDate = new DateTime(DateTime.now.getMillis + testSecondsUntilExpires * 1000)

  trait TestSuccessfulAccessTokenSupport extends AccessTokenSupport {
    val accessTokenResponseJson = s"""{"scope":"$testTokenScope","expires_in":$testSecondsUntilExpires,"token_type":"$testTokenType","access_token":"$testToken"}"""
    override def sendAndReceive = (req: HttpRequest) => Promise.successful(HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentType(MediaTypes.`application/json`, `UTF-8`), accessTokenResponseJson))).future
  }

  trait TestUnauthorizedAccessTokenSupport extends AccessTokenSupport {
    override def sendAndReceive = (req: HttpRequest) => Promise.successful(HttpResponse(status = StatusCodes.Unauthorized)).future
  }

  trait TestInternalErrorAccessTokenSupport extends AccessTokenSupport {
    override def sendAndReceive = (req: HttpRequest) => Promise.successful(HttpResponse(status = StatusCodes.InternalServerError)).future
  }

  trait TestExceptionAccessTokenSupport extends AccessTokenSupport {
    override def sendAndReceive = (req: HttpRequest) => Promise.failed(new Exception("ooopsie... something bad happened")).future
  }

  trait TestParsingErrorAccessTokenSupport extends AccessTokenSupport {
    val accessTokenResponseJson = s"""{"scopez":"$testTokenScope","expires_in":$testSecondsUntilExpires,"foo":"$testTokenType","bar":"$testToken"}"""
    override def sendAndReceive = (req: HttpRequest) => Promise.successful(HttpResponse(status = StatusCodes.OK, entity = HttpEntity(ContentType(MediaTypes.`application/json`, `UTF-8`), accessTokenResponseJson))).future
  }

  "The actor should be proper created " in {
    system.actorOf(AccessTokenManager.props(config)).isInstanceOf[ActorRef] === true
  }

  "The request token method" must {

    val tokenID = "test-token-id"
    val scopeSet = Set("read")

    "request a token from authentication server" when {
      "a valid token is not in cache and the force flag is deactivated" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestSuccessfulAccessTokenSupport)

        val response = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val accessTokenResponse = Await.result(response, default_waiting_time)

        assert(accessTokenResponse != null)
        assert(accessTokenResponse.token != null)
        assert(accessTokenResponse.token.isRight)

        val accessToken = accessTokenResponse.token.right.get
        assert(accessToken.token === testToken)
        assert(accessToken.tokenType === testTokenType)
        assert(accessToken.expiresIn === testSecondsUntilExpires)

      }

      "a valid token is not in cache and the force flag is active" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestSuccessfulAccessTokenSupport)

        val response = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet), forceNewToken = true)).mapTo[AccessTokenResponse]
        val accessTokenResponse = Await.result(response, default_waiting_time)

        assert(accessTokenResponse != null)
        assert(accessTokenResponse.token != null)
        assert(accessTokenResponse.token.isRight)

        val accessToken = accessTokenResponse.token.right.get
        assert(accessToken.token === testToken)
        assert(accessToken.tokenType === testTokenType)
        assert(accessToken.expiresIn === testSecondsUntilExpires)
      }
    }
    
    "return the updated token in cache" when {
      "one is available and the force flag is not active" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestSuccessfulAccessTokenSupport)
        
        val firstResponse = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val firstAccessTokenResponse = Await.result(firstResponse, default_waiting_time)

        assert(firstAccessTokenResponse != null)
        assert(firstAccessTokenResponse.token != null)
        assert(firstAccessTokenResponse.token.isRight)

        val firstAccessToken = firstAccessTokenResponse.token.right.get
        assert(firstAccessToken.token === testToken)
        assert(firstAccessToken.tokenType === testTokenType)
        assert(firstAccessToken.expiresIn === testSecondsUntilExpires)
        
        val firstExpiresInMillis = firstAccessToken.expiresIn
        val secondResponse = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val secondAccessTokenResponse = Await.result(secondResponse, default_waiting_time)

        assert(secondAccessTokenResponse != null)
        assert(secondAccessTokenResponse.token != null)
        assert(secondAccessTokenResponse.token.isRight)

        val secondAccessToken = secondAccessTokenResponse.token.right.get
        assert(secondAccessToken.token === testToken)
        assert(secondAccessToken.tokenType === testTokenType)
        assert(secondAccessToken.expiresIn <= testSecondsUntilExpires)
        
        val secondExpiresInMillis = secondAccessToken.expiresIn
        assert(firstExpiresInMillis > secondExpiresInMillis)
        
      }
    }

    "return an access token unauthorized error" when {
      "the authentication server failed to authorize the request" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestUnauthorizedAccessTokenSupport)

        val response = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val accessTokenResponse = Await.result(response, default_waiting_time)

        assert(accessTokenResponse != null)
        assert(accessTokenResponse.token != null)
        assert(accessTokenResponse.token.isLeft)
        assert(accessTokenResponse.token.left.get === AccessTokenUnauthorizedError)

      }
    }

    "return an access token parsing error" when {
      "the authentication server answers with an unknown format" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestParsingErrorAccessTokenSupport)

        val response = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val accessTokenResponse = Await.result(response, default_waiting_time)

        assert(accessTokenResponse != null)
        assert(accessTokenResponse.token != null)
        assert(accessTokenResponse.token.isLeft)
        assert(accessTokenResponse.token.left.get === AccessTokenParsingError)

      }
    }

    "return an access token generic error" when {
      "there is a status code distinct from 200 or 401 from authentication server" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestInternalErrorAccessTokenSupport)

        val response = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val accessTokenResponse = Await.result(response, default_waiting_time)

        assert(accessTokenResponse != null)
        assert(accessTokenResponse.token != null)
        assert(accessTokenResponse.token.isLeft)
        assert(accessTokenResponse.token.left.get.isInstanceOf[AccessTokenGenericError])

      }

      "there is an exception being thrown in the code" in {

        val actorRef = TestActorRef(new AccessTokenManager(config) with TestExceptionAccessTokenSupport)

        val response = (actorRef ? AccessTokenRequest(AccessTokenConfiguration(tokenID, scopeSet))).mapTo[AccessTokenResponse]
        val accessTokenResponse = Await.result(response, default_waiting_time)

        assert(accessTokenResponse != null)
        assert(accessTokenResponse.token != null)
        assert(accessTokenResponse.token.isLeft)
        assert(accessTokenResponse.token.left.get.isInstanceOf[AccessTokenGenericError])

      }
    }
  }
}
