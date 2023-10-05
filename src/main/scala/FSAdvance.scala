import FSAdvance.ActorRepository.saveToDB
import FSBasics.Data.{andrewGarfield, tobeyMaguire, tomHolland}
import FSBasics.Model.Actor
import FSBasics.{avengersActors, jlActors, liftedJlActors}
import cats.effect.std.Queue
import cats.effect.{IO, IOApp}
import fs2.{Chunk, INothing, Pipe, Pull, Pure}

import scala.concurrent.duration.DurationInt
import scala.util.Random

object FSAdvance extends IOApp.Simple{


  //lets discuss about pipe
  //pipe is nothing but function which has this definition

//pipe = Stream[F,I] => Stream[F,O]

/*
todo
     Many other streaming libraries define streams
     and transformation in terms of sources, pipes, and sinks.
     A source generates the elements of a stream,
     then transformed through a sequence of stages or pipes,
    and finally consumed by a sink. For example,
    the Akka Stream library has specific types modeling these concepts.
todo
    In fs2, the only available type modeling
    the above streaming concepts is Pipe[F[_], -I, +O].
    Pipe is a type alias
    for the function Stream[F, I] => Stream[F, O].
    So, a Pipe[F[_], I, O] represents nothing
    more than a function between two streams,
    the first emitting elements of type I,
    and the second emitting elements of type O
 */

  //pipe = Stream[F,I] => Stream[F,O]
  val fromActorToStringPipe: Pipe[IO, Actor, String] = in =>
    in.map(actor => s"${actor.firstName} ${actor.lastName}")
//more transformation using pipe
  def toConsole[T]: Pipe[IO, T, Unit] = in =>
    in.evalMap(str => IO.println(str))

  val stringNamesPrinted: fs2.Stream[IO, Unit] =
       jlActors
      .through(fromActorToStringPipe)
      .through(toConsole)


  //ERROR Handling

  /**
     What if pulling a value from a stream fails with an exception?
     For example, let’s introduce a repository persisting an Actor:
   */



  object ActorRepository {
    def saveToDB(actor: Actor): IO[Int] = IO {
      println(s"Saving actor: ${actor.firstName}")
      if (Random.nextInt() % 2 == 0) {
        throw new RuntimeException("Something went wrong during the communication with the persistence layer")
      }
      println(s"Saved.")
      actor.id
    }
  }

  val savedJlActors: fs2.Stream[IO, Int] = jlActors.evalMap(saveToDB)

  /*
      fs2 gives us many choices to handle the error.
      First, we can handle an error returning
      a new stream using the handleErrorWith method:
   */

  /*
  In the above example,
  we react to an error by returning a stream
  that prints the error to the console.
  As we can notice, the elements contained
  in the stream are AnyVal and
  not Unit because of the definition of the handleErrorWith:

  def handleErrorWith[F2[x] >: F[x], O2 >: O](h: Throwable => Stream[F2, O2]): Stream[F2, O2] = ???
   The O2 type must be a supertype of O’s original type.
   Since both Int and Unit are subtypes of AnyVal,
   we can use the AnyVal type (the least common supertype)
   to represent the resulting stream.
   */
  val errorHandledSavedJlActors: fs2.Stream[IO, AnyVal] =
                                  savedJlActors
                                    .handleErrorWith(error => fs2.Stream.eval(

                                      IO{
                                        println(s"Error: $error")

                                      }


                                    )

                                    )



  /*
TODO
     Another available method to handle errors in streams is attempt.
     The method works using the scala.Either type,
     which returns a stream of Either elements.
     The resulting stream pulls elements wrapped
     in a Right instance
     until the first error occurs,
     which is nothing more than
     an instance of a Throwable wrapped in a Left
   */

  val attemptedSavedJlActors: fs2.Stream[IO, Either[Throwable, Int]] =savedJlActors.attempt

  /*
  // fs2 library code
     def attempt: Stream[F, Either[Throwable, O]] =
  map(Right(_): Either[Throwable, O]).handleErrorWith(e => Stream.emit(Left(e)))
   */
  val attemptedProcessed: fs2.Stream[IO, Unit] =attemptedSavedJlActors.evalMap {
    case Left(error) => IO.println(s"Error: $error")
    case Right(id) => IO.println(s"Saved actor with id: $id")
  }



  /*
TODO
       Resource Management:
       As the official documentation says,
       we don’t have to use the handleErrorWith method
       to manage the release of resources used by the stream eventually.
       Instead, the fs2 library implements the bracket pattern to manage resources.
       The bracket pattern is a resource management pattern
       developed in the functional programming domain many years ago.
      The pattern defines two functions:
     The first is used to acquire a resource;
     The second is guaranteed to be called when the resource is no longer needed.
       def bracket[F[x] >: Pure[x], R](acquire: F[R])(release: R => F[Unit]): Stream[F, R] = ???

   */

//resource
case class DatabaseConnection(url: String) extends AnyVal

/**
TODO
      We will use the DatabaseConnection as the resource
      we want to acquire and release through the bracket pattern.
      Then, the acquiring and releasing function:
 */

  def acquireConnection(url:String): IO[DatabaseConnection]= IO{
    println("Getting DB Connection!!!")
    DatabaseConnection(url)
  }

 def releaseDBConnection(conn:DatabaseConnection):IO[Unit]=IO{
   println(s"Releasing the Connection!! to ${conn.url}")
 }

  //Finally, we use them to call the bracket method and then save the actors in the stream:
//  val savedJlActors: fs2.Stream[IO, Int] = jlActors.evalMap(saveToDB)
  val managedJlActors: fs2.Stream[IO, Int] =
    fs2
      .Stream
      .bracket(acquireConnection("jdbc://mydb.com"))(releaseDBConnection)
      .flatMap{conn =>
        //process the stream using the resource conn
        savedJlActors.evalTap(actorId=> IO(s"saving Actor with ${actorId}"))
        }

  //merge
  /*
      One of the most used fs2 features is
      using concurrency in streams.
      It’s possible to implement many concurrency patterns
      using the primitives provided by the Stream type.
      The first function we will analyze is merge,
      which lets us run two streams concurrently,
      collecting the results in a single stream
      as they are produced.
      The execution halts when both streams have halted.
      For example,
      we can merge JLA and Avengers concurrently,
      adding some sleep to the execution of each stream
   */
  val concurrentJlActors=jlActors.evalMap{
    actor=> IO{
      Thread.sleep(400)
      actor
    }
  }
  val liftedAvengersActors: fs2.Stream[IO, Actor] = avengersActors.covary[IO]

  val concurrentAvengersActors=liftedAvengersActors.evalMap{
    actor=> IO{
      Thread.sleep(200)
      actor
    }
  }
  val mergedActors: fs2.Stream[IO, Unit] = concurrentJlActors.merge(concurrentJlActors).through(toConsole)

  /**
 TODO
  Since the savedJlActors we defined some time
  ago throws an exception when the stream is evaluated,
  the managedJlActors stream will terminate with an error.
  The release function is called, and the conn value is released.
  The execution output of the managedJlActors stream
 TODO
  should be something similar to the following:
   Acquiring connection to the database: DatabaseConnection(jlaConnection)
   Saving actor: Actor(0,Henry,Cavill)
   Releasing connection to the database: DatabaseConnection(jlaConnection)
   java.lang.RuntimeException: Something went wrong during the communication with the persistence layer
   */

//now lets discuss   The Pull Type

/*
    As we said more than once,
    the fs2 defines streams as a pull type,
    which means that the stream effectively
    computes the next stream element just in time.
TODO
     Under the hood, the library implements
     the Stream type functions
     using the Pull type to honor this behavior.
     This type, also available as a public API,
     lets us implement streams using the pull model.
TODO
       The Pull[F[_], O, R] type represents a program
       that can pull output values of type O
       while computing a result of type R while
       using an effect of type F.
       As we can see, the type introduces
       the new type variable R that is not available in the Stream type.

TODO
     The result R represents the information
     available after the emission of the element
     of type O that should be used
     to emit the next value of a stream.
     For this reason,
     using Pull directly means
     to develop recursive programs.
     Ok, one step at a time.
     Let’s analyze the Pull type and its methods first.
TODO
    The Pull type represents
    a stream as a head and a tail,
    much like we can describe a list.
    The element of type O emitted by the Pull
   represents the head.
  However, since a stream is a possible
  infinite data structure,
  we cannot express it with a finite one.
  So, we return a type R,
 which is all the information
 that we need to compute the tail of the stream.
TODO
     Let’s look at the Pull type methods without further ado.
     The first method we encounter is the smart constructor
     output1, which creates a Pull
     that emits a single value of type O and then completes.
 */

val tomHollandActorPull: Pull[Pure, Actor, Unit] = Pull.output1(tomHolland)

  /*
  We can convert a Pull having the R type
  variable bound to Unit directly to a Stream
   by using the stream method:
   */
  val tomHollandActorStream: fs2.Stream[Pure, Actor] = tomHollandActorPull.stream

/*
TODO
   Revamping the analogy with the finite collection,
   a Pull that returns Unit is like a List with a head and empty tail.

TODO
   Unlike the Stream type, which defines a monad instance
   on the type variable O,
   a Pull forms a monad instance on R.
   If we think, it’s logical:
   All we want is to concatenate the information
   that allows us to compute the tail of the stream.
   So, if we want to create a sequence
   of Pull containing all the actors that play Spider-Man, we can do the following
 */

  val spiderMenActorPull: Pull[Pure, Actor, Unit] =
    tomHollandActorPull >> Pull.output1(tobeyMaguire) >> Pull.output1(andrewGarfield)

  val avengersActorsPull: Pull[Pure, Actor, Unit] = avengersActors.pull.echo
  /*
         In the above example,
         the first invoked function is pull,
         which returns a ToPull[F, O] type.
         This last type is a wrapper around the Stream type,
         which aim is to group
         all functions concerning the conversion into a Pull instance:
         // fs2 library code
       final class ToPull[F[_], O] private[Stream] (
        private val self: Stream[F, O]) extends AnyVal
        def echo: Pull[F, O, Unit] = self.underlying
         The echo function returns the internal representation of the Stream,
         called underlying since a stream is represented as a pull internally:
         final class Stream[+F[_], +O] private[fs2] (private[fs2] val underlying: Pull[F, O, Unit])

   */


  /*
TODO
      Wow! It’s a very intricate type. Let’s describe it step by step.
      First, since the original stream uses the Pure effect,
      the resulting Pull also uses the same effect.
      Then, since the Pull deconstructs the original stream,
     it cannot emit any value,
     and so the output type is INothing (type alias for scala Nothing).
    It follows the value returned by the Pull,
    i.e., the deconstruction of the original Stream.

     The returned value is an Option because
     the Stream may be empty:
     If there is no more value in the original Stream,
     we will have a None. Otherwise,
     we will have the head of the stream
     as a Chunk   head-> Option[(Chunk[Actor]
     and a Stream representing  tail -> fs2.Stream[Pure, Actor])]
     the tail of the original stream.

A variant of the original uncons method returns not the first Chunk but the first stream element. It’s called uncons1:
   */
//Option[(Chunk[Actor], fs2.Stream[Pure, Actor])] this is tail to be evaluated
  val unconsAvengersActors: Pull[Pure, INothing, Option[(Chunk[Actor], fs2.Stream[Pure, Actor])]] =
    avengersActors.pull.uncons

  /*
         A variant of the original uncons method
         returns not the first Chunk
         but the first stream element.
         It’s called uncons1:
   */
  val uncons1AvengersActors: Pull[Pure, INothing, Option[(Actor, fs2.Stream[Pure, Actor])]] =
    avengersActors.pull.uncons1


/*
     With these bullets in our gun,
     it’s time to write down some functions
     that use the Pull type.
     Due to the structure of the type,
    the functions implemented using
    the Pull type are often recursive.

     For example,
     without using the Stream.filter method,
     we can write a pipe filtering
     from a stream of actors,
     all of them with a given first name:
 */

  def takeByName(name: String): Pipe[IO, Actor, Actor] = {
    def go(s: fs2.Stream[IO, Actor], name: String): Pull[IO, Actor, Unit] =
      s.pull.uncons1.flatMap {
        case Some((hd, tl)) =>
          if (hd.firstName == name) Pull.output1(hd) >> go(tl, name)
          else go(tl, name)
        case None => Pull.done
      }

    in => go(in, name).stream
  }

  /*
     As we may expect, the stream contains
     an actor of the JLA more or
     less every two actors of the Avengers.
     Once the Avengers actors are finished,
     the JLA actors fulfill the rest of the stream.
     If we don’t care about the results
     of the second stream running concurrently,
     we can use the concurrently method instead.
     An everyday use case for this method is
     implementing a producer-consumer pattern.
     For example, we can implement a program
     with a producer that uses
     a cats.effect.std.Queue to share JLA actors with a consumer,
     which prints them to the console:
 */
//buffer with 10 elements
val queue: IO[Queue[IO, Actor]] = Queue.bounded[IO, Actor](10)

  val concurrentlyStreams: fs2.Stream[IO, Unit] = fs2.Stream.eval(queue).flatMap { q =>
//val liftedJlActors: Stream[IO, Actor]

    val producer: fs2.Stream[IO, Unit] =
      liftedJlActors
        .evalTap(actor => IO.println(s"[${Thread.currentThread().getName}] produced $actor"))
        .evalMap(actor=> q.offer(actor))//enque
        .metered(1.second)// throttle at 1 effect per second

    val consumer: fs2.Stream[IO, Unit] =
         fs2.Stream
        .fromQueueUnterminated(q) //deque
        .evalMap(actor => IO.println(s"[${Thread.currentThread().getName}] consumed $actor"))


    producer.concurrently(consumer)
  }
  override def run: IO[Unit] = concurrentlyStreams.compile.drain
}
