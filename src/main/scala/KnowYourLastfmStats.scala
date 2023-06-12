package know.your.lastfm.stats

import zio.*
import zio.http.*
import zio.http.Server
import zio.*
import zio.http.Header.{AccessControlAllowMethods, AccessControlAllowOrigin, Origin}
import zio.http.internal.middlewares.Cors.CorsConfig

object KnowYourLastfmStats extends ZIOAppDefault {
  val port = 9000

  val url = "http://sports.api.decathlon.com/groups/water-aerobics"

  val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> Root / "owl" =>
      val response = Client.request(url)
      println("ALLO 1")
      Console.printLine(response.map(_.body.asString))
      for {
        res <- Client.request(url)
        _ = println("ALLO 2")
        data <- res.body.asString
        _ = println("ALLO")
        _ = println(data)
        _ <- Console.printLine(data)
      } yield data

      Response.text("Hello, ZIO owls!")
    case Method.GET -> Root / "owls" =>
      Response.json("""{"greetings": "Hello World!"}""")
  }


  //request -> middleware -> combined

  //  val loggingHttp = app @@ Verbose.log

  val corsConfig = CorsConfig(
    allowedMethods = AccessControlAllowMethods(Method.GET),
    allowedOrigin = {
      case origin@Origin.Value(_, host, _) if host == "localhost" => Some(AccessControlAllowOrigin.Specific(origin))
      case _ => None
    })

  //  val corsEnabledHttp = app @@ Middleware.cors(corsConfig) @@ Verbose.log

  //  val httpProgram = for {
  //    _ <- Console.printLine(s"Starting server at http://localhost:$port")
  //    _ <- Server.start(port, corsEnabledHttp)
  //  } yield ()

  //  val program = for {
  //    res <- Client.request(url)
  //    data <- res.body.asString
  //    _ <- Console.printLine(data)
  //  } yield ()


  //  override def run = httpProgram

  val program = for {
    res <- Client.request(url)
    data <- res.body.asString
    _ <- Console.printLine(data)
  } yield ()

  val run = for {
    _ <- Server.serve(app).provide(Server.default)
    _ <- program.provide(Client.default)
  } yield ()


}


//request -> cintramap -> http app -> response -> map -> final response
//object Verbose {
//  def log[R, E >: Exception]: Middleware[R, E, Request, Response, Request, Response] = new Middleware {
//    override def apply[R1 <: R, E1 >: E](http: Http[R1, E1, Request, Response]): Http[R1, E1, Request, Response] =
//      http
//        .contramapZIO[R1, E1, Request]{ request =>
//          for {
//            _ <- Console.printLine(s"> ${request.method} ${request.path} ${request.version}")
//            _ <- ZIO.foreach(request.headers.toList) { header =>
//              Console.printLine(s"> ${header._1} ${header._2}")
//            }
//          } yield request
//        }
//        .mapZIO[R1, E1, Response]( response => for {
//        _ <- Console.printLine(s"< ${response.status}")
//        _ <- ZIO.foreach(response.headers.toList){ header =>
//          Console.printLine(s"< ${header._1} ${header._2}")
//        }
//      } yield response
//      )
//  }
//}
