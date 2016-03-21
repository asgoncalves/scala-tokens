package pt.sergio.scalatokens

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

trait ConfigSupport {

  def loadedConfig: Config

  val userFilePath = loadedConfig.getString("oauth.credentials.user")
  val clientFilePath = loadedConfig.getString("oauth.credentials.client")
  val oauthUrl = loadedConfig.getString("oauth.url")
  val grantType = loadedConfig.getString("oauth.grant-type")
  val realm = loadedConfig.getString("oauth.realm")
  val defaultTimeout = loadedConfig.getDuration("oauth.ask-timeout", TimeUnit.SECONDS)
  val defaultEncoding = loadedConfig.getString("oauth.encoding")
  val minimumValidDuration = loadedConfig.getDuration("oauth.minimum-duration", TimeUnit.MILLISECONDS)
}
