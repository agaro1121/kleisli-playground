package io.agaro.testing

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import natchez._
import natchez.log.Log
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.HttpRoutes
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import scala.concurrent.ExecutionContext
import natchez.Tags

class SomeDependency[F[_] : Trace : Sync] {
  def doStuff: F[Unit] = Trace[F].span("do-stuff") {
    Sync[F].delay(println("doing stuffs"))
  }
}

final class SampleService[F[_] : Trace : Sync](dep: SomeDependency[F]) {
  def doSampleStuff: F[Unit] = Trace[F].span("do-sample-stuff") {
    Trace[F].put(Tags.db.user("agaro")) >>
      dep.doStuff >> Sync[F].delay(println("doing sample stuffs"))
  }
}

object SampleRoutes {

  class Routes[F[_] : Defer : Monad : Trace](
                                              service: SampleService[F]
                                            ) extends Http4sDsl[F] {
    val routes = HttpRoutes.of[F] {
      case req@GET -> Root =>
        Trace[F].span("root") {
          Trace[F].put(Tags.http.method(req.method.name)) >>
            service.doSampleStuff >> Ok("success").flatTap(
            r => Trace[F].put(Tags.http.status_code(r.status.code.toString))
          )
        }

    }
  }

}

object TestApp extends IOApp {

  type TraceIO[A] = Kleisli[IO, Span[IO], A]

  implicit def unsafeLogger[F[_] : Sync] = Slf4jLogger.getLogger[F]

  val sampleService = new SampleService[TraceIO](new SomeDependency[TraceIO] {})

  val tracedRoutes: Kleisli[TraceIO, Request[TraceIO], Response[TraceIO]] =
    new SampleRoutes.Routes[TraceIO](sampleService).routes.orNotFound

  val to = new (TraceIO ~> IO) {
    override def apply[A](fa: TraceIO[A]): IO[A] = {
      Log.entryPoint[IO]("test-service") // how to manage this entryPoint properly?
        .root("testing-to").use(span => fa.run(span))
    }
  }

  val from = new (IO ~> TraceIO) {
    override def apply[A](fa: IO[A]): TraceIO[A] =
      Kleisli.liftF[IO, Span[IO], A](fa) // no span?
  }

  // HttpApp[TraceIO] ~> HttpApp[IO]
  val app = tracedRoutes.translate[IO](to)(from)

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](ExecutionContext.global)
      .withHttpApp(app)
      .bindLocal(9000)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}