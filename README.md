Scala tokens library
========================

This library has the purpose of handling the request of OAuth2 tokens.

## Dependencies
This library is built upon the following dependencies:

   * akka.actor

   * akka-agent

   * akka-testkit

   * scalatest

   * spray-client

   * spray-json

   * joda-time

###TODO: add here how to grab and add to projects the latest version of the lib

Follows a list of the current dependencies versions used:

```scala
   libraryDependencies ++= Seq(
     "org.scalatest"        %% "scalatest"     % "2.2.4",
     "com.typesafe.akka"    %% "akka-agent"    % "2.3.12",
     "com.typesafe.akka"    %% "akka-actor"    % "2.3.12",
     "com.typesafe.akka"    %% "akka-testkit"  % "2.3.12" % "test",
     "io.spray"             %% "spray-json"    % "1.3.2",
     "io.spray"             %% "spray-client"  % "1.3.3",
     "joda-time"            % "joda-time"      % "2.8.1"
)
```

### Initialize the `AccessTokenManagementActor`

```scala
val accessTokenManagementActor = system.actorOf(Props[AccessTokenManager])
```

### Use

```scala
val tokenFuture = (accessTokenManagementActor ? AccessTokenRequest(AccessTokenConfiguration("token-id", List("read", "write"))), false))
tokenFuture.mapTo[AccessTokenResponse].onComplete {
  case Success(accessTokenResponse) => // do something with your shiny token
  case Failure(error) => // it seems like something bad happened - recover
}
```

## What improvements to expect in the near future
  * Token management:
    * Start the `AccessTokenManagementActor` with all the tokens to be managed even before the need for a token
    * Automatically refresh the token within the `AccessTokenManagementActor` without the need of a request
    * hash the tokens-ids with the scopes in the cache so that it can have multiple tokens with same ID but different scopes
  * Beautify the code to be more compliant with scala best practices and increase resilience
  * Add logging

## License

The MIT License (MIT)

Copyright (c) 2015

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
