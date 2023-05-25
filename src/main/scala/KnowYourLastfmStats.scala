package know.your.lastfm.stats

import zio.*
import zhttp.*
import zhttp.http.*
import zhttp.http.middleware.Cors.CorsConfig
import zhttp.service.{Client, Server}
import know.your.lastfm.stats.KnowYourLastfmStats.validateEnv

object KnowYourLastfmStats extends ZIOAppDefault {
  val port = 9000

  val app: Http[Any, Nothing, Request, Response] = Http.collect[Request] {
    case Method.GET -> !! / "owls" => Response.text("Hello, ZIO owls!")
  }

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 5).map(n => Response.text("Hello " * n + ", owls!"))
  }

  val combined = app ++ zApp

  val httpProgram = for {
    _ <- Console.printLine(s"Starting server at http://localhost:$port")
    _ <- Server.start(port, combined)
  } yield ()

  override def run = httpProgram
}

