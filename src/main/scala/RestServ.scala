import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._

object RestServ {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val route = {
      path("path") {
        get {
          parameters('key.as[String], 'value.as[String]) { (key, value) =>
            complete {
              HttpEntity("Say hello to akka-http")
            }
          }
        }
      } ~
      path("path2") {
        get {
          complete {
            HttpEntity("path2")
          }
        }
      }
    }

    val serverSource = Http().bindAndHandle(route, "localhost", 8080)

  }

}


