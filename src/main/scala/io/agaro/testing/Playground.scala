package io.agaro.testing

import cats.data.Kleisli
import cats.effect._
import cats.~>
import natchez.{Span, Trace}
import org.http4s.{HttpApp, Request, Response}

import scala.util.Try

object Playground extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    IO(println("Saluton Mondo!")).as(ExitCode.Success)
}


trait Something[F[_]] {
  val fk = new (F ~> Kleisli[F, Span[F], *]) {
    def apply[A](fa: F[A]): Kleisli[F, Span[F], A] =
      Kleisli.liftF(fa)
  }
}

object KleisliPlayground extends IOApp {

  import cats.effect.implicits._
  import cats.effect._



  trait Span

  val testing: Kleisli[IO, Request[IO], Response[IO]] = ???
  val testing2: Kleisli[IO, Span, Response[IO]] = ???
  val f: Span => Response[IO] = ???

  val res: IO[Response[IO]] = testing2.run(new Span {})

  val ioResponse = IO.pure(Response[IO]())

  val lifted: Kleisli[IO, Span, Response[IO]] =
    Kleisli.liftF[IO, Span, Response[IO]](ioResponse)
  lifted.run(new Span{}) // IO[Response]


  import cats.effect.Sync
  import natchez._
  import cats.implicits._

  def thing[F[_]](service: HttpApp[Kleisli[F, Span, *]])
                 (implicit F: Sync[F]): HttpApp[F] = {

    val liftedRequest =
      Request[F]().mapK(Kleisli.liftK[F, Span])

    val resInF =
      service.run(liftedRequest).run(new Span{})

    val properRes = resInF.map{r =>
      r.mapK(
        new (Kleisli[F, Span, *] ~> F) { def apply[A](a: Kleisli[F, Span, A]): F[A] = a.run(new Span{}) }
      )
    }


    def kleisliTrace[F[_]: Bracket[*[_], Throwable]] = ???
    kleisliTrace[IO]

    Kleisli.applyK[IO, Response[IO]](Response[IO]())
    Kleisli.applyK[IO, Span](new Span{})

    ???
  }

  val r = Kleisli[IO, String, Int](s => IO.fromEither(Either.catchNonFatal(s.toInt))).first[String].run(("1", "2.5"))

  override def run(args: List[String]): IO[ExitCode] =
    r.map(println).as(ExitCode.Success)

}

import cats._
import cats.implicits._

class ThingA[F[_]: Trace: Monad] {

  def doThing = Trace[F].span("someSpan") {
    Monad[F].pure("Saluton Mondo")
  }

}

object Runner extends IOApp {
  val r: IO[IO[Int]] =
    Kleisli[IO, String, Int](s => IO.fromEither(Either.catchNonFatal(s.toInt))).lower.run(("1"))

  override def run(args: List[String]): IO[ExitCode] =
    r.map(println).as(ExitCode.Success)
}
