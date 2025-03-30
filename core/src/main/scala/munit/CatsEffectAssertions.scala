/*
 * Copyright 2021 Typelevel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package munit

import cats.effect.IO
import cats.syntax.eq._
import scala.reflect.ClassTag
import scala.util.control.NonFatal
import cats.effect.SyncIO
import cats.effect.Sync

trait CatsEffectAssertions { self: Assertions =>

  /** Asserts that an `IO` returns an expected value.
    *
    * The "returns" value (second argument) must have the same type or be a subtype of the one
    * "contained" inside the `IO` (first argument). For example:
    * {{{
    *   assertIO(IO(Option(1)), returns = Some(1)) // OK
    *   assertIO(IO(Some(1)), returns = Option(1)) // Error: Option[Int] is not a subtype of Some[Int]
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the assertion
    * fails.
    *
    * @param obtained
    *   the IO under testing
    * @param returns
    *   the expected value
    * @param clue
    *   a value that will be printed in case the assertions fails
    */
  def assertIO[A, B](
      obtained: IO[A],
      returns: B,
      clue: => Any = "values are not the same"
  )(implicit loc: Location, ev: B <:< A): IO[Unit] =
    obtained.flatMap(a => IO(assertEquals(a, returns, clue)))

  /** Asserts that an `IO[Unit]` returns the Unit value.
    *
    * For example:
    * {{{
    *   assertIO_(IO.unit)
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the assertion
    * fails.
    *
    * @param obtained
    *   the IO under testing
    * @param clue
    *   a value that will be printed in case the assertions fails
    */
  protected def assertIO_(
      obtained: IO[Unit],
      clue: => Any = "value is not ()"
  )(implicit loc: Location): IO[Unit] =
    obtained.flatMap(a => IO(assertEquals(a, (), clue)))

  /** Asserts that an `IO[Boolean]` returns true.
    *
    * For example:
    * {{{
    *   assertIOBoolean(IO(true))
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the assertion
    * fails.
    *
    * @param obtained
    *   the IO[Boolean] under testing
    * @param clue
    *   a value that will be printed in case the assertions fails
    */
  protected def assertIOBoolean(
      obtained: IO[Boolean],
      clue: => Any = "values are not the same"
  )(implicit loc: Location): IO[Unit] =
    assertIO(obtained, true, clue)

  /** Intercepts a `Throwable` being thrown inside the provided `IO`.
    *
    * @example
    *   {{{
    *   val io = IO.raiseError[Unit](MyException("BOOM!"))
    *
    *   interceptIO[MyException](io)
    *   }}}
    *
    * or
    *
    * {{{
    *   interceptIO[MyException] {
    *       IO.raiseError[Unit](MyException("BOOM!"))
    *   }
    * }}}
    */
  def interceptIO[T <: Throwable](io: IO[Any])(implicit T: ClassTag[T], loc: Location): IO[T] =
    io.attempt.flatMap[T](runInterceptF[IO, T](None))

  /** Intercepts a `Throwable` with a certain message being thrown inside the provided `IO`.
    *
    * @example
    *   {{{
    *   val io = IO.raiseError[Unit](MyException("BOOM!"))
    *
    *   interceptIO[MyException]("BOOM!")(io)
    *   }}}
    *
    * or
    *
    * {{{
    *   interceptIO[MyException] {
    *       IO.raiseError[Unit](MyException("BOOM!"))
    *   }
    * }}}
    */
  def interceptMessageIO[T <: Throwable](
      expectedExceptionMessage: String
  )(io: IO[Any])(implicit T: ClassTag[T], loc: Location): IO[T] =
    io.attempt.flatMap[T](runInterceptF[IO, T](Some(expectedExceptionMessage)))

  /** Asserts that a `SyncIO` returns an expected value.
    *
    * The "returns" value (second argument) must have the same type or be a subtype of the one
    * "contained" inside the `SyncIO` (first argument). For example:
    * {{{
    *   assertSyncIO(SyncIO(Option(1)), returns = Some(1)) // OK
    *   assertSyncIO(SyncIO(Some(1)), returns = Option(1)) // Error: Option[Int] is not a subtype of Some[Int]
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the assertion
    * fails.
    *
    * @param obtained
    *   the SyncIO under testing
    * @param returns
    *   the expected value
    * @param clue
    *   a value that will be printed in case the assertions fails
    */
  def assertSyncIO[A, B](
      obtained: SyncIO[A],
      returns: B,
      clue: => Any = "values are not the same"
  )(implicit loc: Location, ev: B <:< A): SyncIO[Unit] =
    obtained.flatMap(a => SyncIO(assertEquals(a, returns, clue)))

  /** Asserts that a `SyncIO[Unit]` returns the Unit value.
    *
    * For example:
    * {{{
    *   assertSyncIO_(SyncIO.unit) // OK
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the assertion
    * fails.
    *
    * @param obtained
    *   the SyncIO under testing
    * @param clue
    *   a value that will be printed in case the assertions fails
    */
  protected def assertSyncIO_(
      obtained: SyncIO[Unit],
      clue: => Any = "value is not ()"
  )(implicit loc: Location): SyncIO[Unit] =
    obtained.flatMap(a => SyncIO(assertEquals(a, (), clue)))

  /** Asserts that a `SyncIO[Boolean]` returns true.
    *
    * For example:
    * {{{
    *   assertSyncIOBoolean(SyncIO(true))
    * }}}
    *
    * The "clue" value can be used to give extra information about the failure in case the assertion
    * fails.
    *
    * @param obtained
    *   the SyncIO[Boolean] under testing
    * @param clue
    *   a value that will be printed in case the assertions fails
    */
  protected def assertSyncIOBoolean(
      obtained: SyncIO[Boolean],
      clue: => Any = "values are not the same"
  )(implicit loc: Location): SyncIO[Unit] =
    assertSyncIO(obtained, true, clue)

  /** Intercepts a `Throwable` being thrown inside the provided `SyncIO`.
    *
    * @example
    *   {{{
    *   val io = SyncIO.raiseError[Unit](MyException("BOOM!"))
    *
    *   interceptSyncIO[MyException](io)
    *   }}}
    *
    * or
    *
    * {{{
    *   interceptSyncIO[MyException] {
    *       SyncIO.raiseError[Unit](MyException("BOOM!"))
    *   }
    * }}}
    */
  def interceptSyncIO[T <: Throwable](
      io: SyncIO[Any]
  )(implicit T: ClassTag[T], loc: Location): SyncIO[T] =
    io.attempt.flatMap[T](runInterceptF[SyncIO, T](None))

  /** Intercepts a `Throwable` with a certain message being thrown inside the provided `SyncIO`.
    *
    * @example
    *   {{{
    *   val io = SyncIO.raiseError[Unit](MyException("BOOM!"))
    *
    *   interceptSyncIO[MyException]("BOOM!")(io)
    *   }}}
    *
    * or
    *
    * {{{
    *   interceptSyncIO[MyException] {
    *       SyncIO.raiseError[Unit](MyException("BOOM!"))
    *   }
    * }}}
    */
  def interceptMessageSyncIO[T <: Throwable](
      expectedExceptionMessage: String
  )(io: SyncIO[Any])(implicit T: ClassTag[T], loc: Location): SyncIO[T] =
    io.attempt.flatMap[T](runInterceptF[SyncIO, T](Some(expectedExceptionMessage)))

  /** Copied from `munit.Assertions` and adapted to return `IO[T]` instead of `T`.
    */
  private def runInterceptF[F[_]: Sync, T <: Throwable](
      expectedExceptionMessage: Option[String]
  )(implicit T: ClassTag[T], loc: Location): Either[Throwable, Any] => F[T] = {
    case Right(value) =>
      Sync[F].delay {
        fail(
          s"expected exception of type '${T.runtimeClass.getName}' but body evaluated successfully",
          clues(value)
        )
      }
    case Left(e: FailException) if !T.runtimeClass.isAssignableFrom(e.getClass) =>
      Sync[F].raiseError[T](e)
    case Left(NonFatal(e: T)) if expectedExceptionMessage.forall(_ === e.getMessage) =>
      Sync[F].pure(e)
    case Left(NonFatal(e: T)) =>
      Sync[F].raiseError[T] {
        val obtained = e.getClass.getName

        new FailException(
          s"intercept failed, exception '$obtained' had message '${e.getMessage}', " +
            s"which was different from expected message '${expectedExceptionMessage.get}'",
          cause = e,
          isStackTracesEnabled = false,
          location = loc
        )
      }
    case Left(NonFatal(e)) =>
      Sync[F].raiseError[T] {
        val obtained = e.getClass.getName
        val expected = T.runtimeClass.getName

        new FailException(
          s"intercept failed, exception '$obtained' is not a subtype of '$expected",
          cause = e,
          isStackTracesEnabled = false,
          location = loc
        )
      }
    case Left(e) =>
      Sync[F].raiseError[T](e)
  }

  private def mapOrFailF[F[_], A, B](
      io: F[A],
      pf: PartialFunction[A, B],
      clue: => Any
  )(implicit F: Sync[F], loc: Location): F[B] =
    F.flatMap(io) { a =>
      // It could be just "case `pf`(b) => F.pure(b)" but 2.12 doesn't define `unapply` for `PartialFunction`.
      pf.andThen(F.pure[B])
        .applyOrElse[A, F[B]](
          a,
          aa =>
            F.raiseError(
              new FailException(
                s"${munitPrint(clue)}, value obtained: $aa",
                location = loc
              )
            )
        )
    }

  implicit class MUnitCatsAssertionsForIOOps[A](io: IO[A]) {

    /** Asserts that this effect returns an expected value.
      *
      * The "expected" value (second argument) must have the same type or be a subtype of the one
      * "contained" inside the effect. For example:
      * {{{
      *   IO(Option(1)).assertEquals(Some(1)) // OK
      *   IO(Some(1)).assertEquals(Option(1)) // Error: Option[Int] is not a subtype of Some[Int]
      * }}}
      *
      * The "clue" value can be used to give extra information about the failure in case the
      * assertion fails.
      *
      * @param expected
      *   the expected value
      * @param clue
      *   a value that will be printed in case the assertions fails
      */
    def assertEquals[B](
        expected: B,
        clue: => Any = "values are not the same"
    )(implicit loc: Location, ev: B <:< A): IO[Unit] =
      assertIO(io, expected, clue)

    /** Asserts that this effect satisfies a given predicate.
      *
      * {{{
      *  IO.pure(1).assert(_ > 0)
      * }}}
      *
      * The "clue" value can be used to give extra information about the failure in case the
      * assertion fails.
      *
      * @param pred
      *   the predicate that must be satisfied
      * @param clue
      *   a value that will be printed in case the assertions fails
      */
    def assert(pred: A => Boolean, clue: => Any = "predicate not satisfied")(implicit
        loc: Location
    ): IO[Unit] =
      assertIOBoolean(io.map(pred), clue)

    /** Maps a value from this effect with a given `PartialFunction` or fails if the value doesn't
      * match. Then the mapped value can be used for further processing or validation.
      *
      * This method can come in handy in complex validation scenarios where multi-step assertions
      * are necessary.
      *
      * @example
      *   {{{
      *   case class Response(status: Int, body: IO[Array[Byte]])
      *
      *   def decodeResponseBytes(bytes: IO[Array[Byte]]): IO[String] =
      *     bytes.map(String.fromBytes(_))
      *
      *   val response: IO[Response] =
      *     IO.pure(Response(
      *       status = 200,
      *       body = IO {
      *         "<expected response body>".getBytes("UTF-8")
      *       }
      *     ))
      *
      *    response
      *      // First, check if the response has the expected status,
      *      // then pass it over to the next step for further processing.
      *      .mapOrFail { case Response(200, body) => body }
      *      // Decode the response body in order to prepare for the final check.
      *      .flatMap(decodeResponseBytes)
      *      // Make sure that the response has the expected content.
      *      .assertEquals("<expected response body>")
      *   }}}
      *
      * @param pf
      *   a partial function that matches the value obtained from the effect
      */
    def mapOrFail[B](
        pf: PartialFunction[A, B],
        clue: => Any = "value didn't match any of the defined cases"
    )(implicit loc: Location): IO[B] =
      mapOrFailF(io, pf, clue)

    /** Intercepts a `Throwable` being thrown inside this effect.
      *
      * @example
      *   {{{
      *   val io = IO.raiseError[Unit](MyException("BOOM!"))
      *
      *   io.intercept[MyException]
      *   }}}
      */
    def intercept[T <: Throwable](implicit T: ClassTag[T], loc: Location): IO[T] =
      interceptIO[T](io)

    /** Intercepts a `Throwable` with a certain message being thrown inside this effect.
      *
      * @example
      *   {{{
      *   val io = IO.raiseError[Unit](MyException("BOOM!"))
      *
      *   io.intercept[MyException]("BOOM!")
      *   }}}
      */
    def interceptMessage[T <: Throwable](
        expectedExceptionMessage: String
    )(implicit T: ClassTag[T], loc: Location): IO[T] =
      interceptMessageIO[T](expectedExceptionMessage)(io)

  }

  implicit class MUnitCatsAssertionsForIOUnitOps(io: IO[Unit]) {

    /** Asserts that this effect returns the Unit value.
      *
      * For example:
      * {{{
      *   IO.unit.assert // OK
      * }}}
      */
    def assert(implicit loc: Location): IO[Unit] =
      assertIO_(io)
  }

  implicit class MUnitCatsAssertionsForIOBooleanOps(io: IO[Boolean]) {

    /** Asserts that this effect returns an expected value.
      *
      * For example:
      * {{{
      *   IO(true).assert // OK
      * }}}
      */
    def assert(implicit loc: Location): IO[Unit] =
      assertIOBoolean(io, "value is not true")
  }

  implicit class MUnitCatsAssertionsForSyncIOOps[A](io: SyncIO[A]) {

    /** Asserts that this effect returns an expected value.
      *
      * The "expected" value (second argument) must have the same type or be a subtype of the one
      * "contained" inside the effect. For example:
      * {{{
      *   SyncIO(Option(1)).assertEquals(Some(1)) // OK
      *   SyncIO(Some(1)).assertEquals(Option(1)) // Error: Option[Int] is not a subtype of Some[Int]
      * }}}
      *
      * The "clue" value can be used to give extra information about the failure in case the
      * assertion fails.
      *
      * @param expected
      *   the expected value
      * @param clue
      *   a value that will be printed in case the assertions fails
      */
    def assertEquals[B](
        expected: B,
        clue: => Any = "values are not the same"
    )(implicit loc: Location, ev: B <:< A): SyncIO[Unit] =
      assertSyncIO(io, expected, clue)

    /** Asserts that this effect satisfies a given predicate.
      *
      * {{{
      *  SyncIO.pure(1).assert(_ > 0)
      * }}}
      *
      * The "clue" value can be used to give extra information about the failure in case the
      * assertion fails.
      *
      * @param pred
      *   the predicate that must be satisfied
      * @param clue
      *   a value that will be printed in case the assertions fails
      */
    def assert(pred: A => Boolean, clue: => Any = "predicate not satisfied")(implicit
        loc: Location
    ): SyncIO[Unit] =
      assertSyncIOBoolean(io.map(pred), clue)

    /** Maps a value from this effect with a given `PartialFunction` or fails if the value doesn't
      * match. Then the mapped value can be used for further processing or validation.
      *
      * This method can come in handy in complex validation scenarios where multi-step assertions
      * are necessary.
      *
      * @example
      *   {{{
      *   case class Response(status: Int, body: SyncIO[Array[Byte]])
      *
      *   def decodeResponseBytes(bytes: SyncIO[Array[Byte]]): SyncIO[String] =
      *     bytes.map(String.fromBytes(_))
      *
      *   val response: SyncIO[Response] =
      *     SyncIO.pure(Response(
      *       status = 200,
      *       body = SyncIO {
      *         "<expected response body>".getBytes("UTF-8")
      *       }
      *     ))
      *
      *    response
      *      // First, check if the response has the expected status,
      *      // then pass it over to the next step for further processing.
      *      .mapOrFail { case Response(200, body) => body }
      *      // Decode the response body in order to prepare for the final check.
      *      .flatMap(decodeResponseBytes)
      *      // Make sure that the response has the expected content.
      *      .assertEquals("<expected response body>")
      *   }}}
      *
      * @param pf
      *   a partial function that matches the value obtained from the effect
      */
    def mapOrFail[B](
        pf: PartialFunction[A, B],
        clue: => Any = "value didn't match any of the defined cases"
    )(implicit loc: Location): SyncIO[B] =
      mapOrFailF(io, pf, clue)

    /** Intercepts a `Throwable` being thrown inside this effect.
      *
      * @example
      *   {{{
      *   val io = SyncIO.raiseError[Unit](MyException("BOOM!"))
      *
      *   io.intercept[MyException]
      *   }}}
      */
    def intercept[T <: Throwable](implicit T: ClassTag[T], loc: Location): SyncIO[T] =
      interceptSyncIO[T](io)

    /** Intercepts a `Throwable` with a certain message being thrown inside this effect.
      *
      * @example
      *   {{{
      *   val io = SyncIO.raiseError[Unit](MyException("BOOM!"))
      *
      *   io.intercept[MyException]("BOOM!")
      *   }}}
      */
    def interceptMessage[T <: Throwable](
        expectedExceptionMessage: String
    )(implicit T: ClassTag[T], loc: Location): SyncIO[T] =
      interceptMessageSyncIO[T](expectedExceptionMessage)(io)

  }

  implicit class MUnitCatsAssertionsForSyncIOUnitOps(io: SyncIO[Unit]) {

    /** Asserts that this effect returns the Unit value.
      *
      * For example:
      * {{{
      *   SyncIO.unit.assert // OK
      * }}}
      */
    def assert(implicit loc: Location): SyncIO[Unit] =
      assertSyncIO_(io)
  }
}

object CatsEffectAssertions extends Assertions with CatsEffectAssertions
