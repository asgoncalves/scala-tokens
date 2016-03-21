package pt.sergio.scalatokens

import akka.actor.{Actor, ActorRef, Props}
import akka.agent.Agent
import com.typesafe.config.Config
import org.joda.time.DateTime
import pt.sergio.scalatokens.domain._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object AccessTokenManager {
  def props(config: Config): Props = Props(new AccessTokenManager(config))
}

class AccessTokenManager(val config: Config) extends Actor with AccessTokenSupport {
  
  implicit val actorSystem = context.system
  import context.dispatcher
  
  /** The [[AccessTokenConfiguration]] cache */
  val tokenConfigurationsPerTokenId = Agent(Map[String, AccessTokenConfiguration]())

  /** Simply checks the received messages and forwards them */
  override def receive: Receive = {
    
    case AccessTokenRequest(accessTokenConfiguration, forceNewToken) => getAccessTokenForScope(accessTokenConfiguration, forceNewToken, sender())

  }

  /** Retrieves an [[AccessToken]] from either cache or requests a new one */
  private def getAccessTokenForScope(accessTokenConfiguration: AccessTokenConfiguration, forceNewToken: Boolean, sender: ActorRef) = {

    // check if there is a cached token and if it complies with configuration and request
    val accessTokenResponseFuture: Future[Either[AccessTokenError, AccessToken]] = if (forceNewToken ||
                          !tokenConfigurationsPerTokenId().contains(accessTokenConfiguration.tokenId) ||
                          !isAccessTokenValid(tokenConfigurationsPerTokenId()(accessTokenConfiguration.tokenId))) {
      requestNewAccessToken(accessTokenConfiguration)
    } else {
      getCachedAccessTokenForScope(accessTokenConfiguration.tokenId)
    }

    accessTokenResponseFuture.onComplete {
      case Success(accessTokenResponse: Either[AccessTokenError, AccessToken]) =>
        accessTokenResponse match {
          case Right(accessToken) =>
            val updatedAccessTokenConfig = accessTokenConfiguration.copy(token = Some(accessToken), validUntil = Some(new DateTime(DateTime.now().getMillis + (accessToken.expiresIn * 1000))))

            // add the configuration to the cache
            tokenConfigurationsPerTokenId send {
              _ + (updatedAccessTokenConfig.tokenId -> updatedAccessTokenConfig)
            }
          case Left(_) => // send the response to the sender without updating
        }

        sender ! AccessTokenResponse(accessTokenResponse)

      case Failure(error) => sender ! AccessTokenResponse(Left(AccessTokenGenericError(error.getMessage)))
    }
  }

  /** Returns a cached [[AccessToken]] for the given token ID */
  private def getCachedAccessTokenForScope(tokenId: String): Future[Either[AccessTokenError, AccessToken]] = {
    val accessTokenConfiguration = tokenConfigurationsPerTokenId()(tokenId)
    Future(Right(accessTokenConfiguration.token.get withExpirationTime ((accessTokenConfiguration.validUntil.get.getMillis - System.currentTimeMillis) / 1000)))
  }

  /** Evaluates whether an [[AccessToken]] is valid or not */
  private def isAccessTokenValid(accessTokenConfiguration: AccessTokenConfiguration): Boolean = accessTokenConfiguration.validUntil.get.getMillis - System.currentTimeMillis > minimumValidDuration

  override def loadedConfig: Config = config
}