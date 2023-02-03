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
//    case Method.GET -> !! / "owls" => Response.text("Hello, ZIO!")
    case Method.GET -> !! / "owls" => for {
        r <- Client.request(
        "" //,
        //Headers.host("sports.api.decathlon.com")
        )
        data <- r.body.asString
      } yield Response.text(data)
  }

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "owls" =>
      Random.nextIntBetween(3, 5).map(n => Response.text("Hello " * n + ", ZIOs!"))
  }

  //TODO: orElse?
  val combined = app ++  zApp

  //request -> middleware -> combined
  val wrapped = combined @@ Middleware.debug

  val loggingHttp = combined @@ Verbose.log

  //CORS
  val corsConfig = CorsConfig(
    anyOrigin = false,
    anyMethod = false,
    allowCredentials = ???,
    allowedOrigins = _.equals("localhost"),
    allowedMethods = Some(Set(Method.GET, Method.POST))
  )

  val corsEnabledHttp = combined @@ Middleware.cors(corsConfig) @@ Verbose.log

  //CSRF

  val httpProgram = for {
    _ <- Console.printLine(s"Starting server at http://localhost:$port")
    _ <- Server.start(port, corsEnabledHttp)
  } yield ()

  override def run = httpProgram
}

//TODO: move to middleware package
object Verbose {
  def log[R, E >: Exception]: Middleware[R, E, Request, Response, Request, Response] = new Middleware[R, E, Request, Response, Request, Response] {
    override def apply[R1 <: R, E1 >: E](http: Http[R1, E1, Request, Response]): Http[R1, E1, Request, Response] =
      //TODO: map vs mapZIO?
      http
        .contramapZIO[R1, E1, Request]{ request =>
          for {
          _ <- Console.printLine(s"> ${request.method} ${request.path} ${request.version}")
          _ <- ZIO.foreach(request.headers.toList) { header =>
            Console.printLine(s"> ${header._1} ${header._2}")
          }
          } yield request
        }
        .mapZIO[R1, E1, Response](response =>
        for {
          _ <- Console.printLine(s"< ${response.status}")
          _ <- ZIO.foreach(response.headers.toList){ header =>
            Console.printLine(s"< ${header._1} ${header._2}")
          }
        } yield response
      )
  }
}
