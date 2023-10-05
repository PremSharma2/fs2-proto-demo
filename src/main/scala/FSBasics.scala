import FSBasics.Model.Actor
import cats.effect.{IO, IOApp}
import fs2.{Pure, Stream}

object FSBasics  extends IOApp.Simple {

  /*

TODO
     FS2 streams are built around a few primary types,
      most notably Stream[F[_], O]:
TODO
     F[_] is an effect type that captures effects.
     Commonly used types for F include cats.effect.IO and other monadic effect types.
     O is the type of output Data Structure  elements produced by the stream.
TODO
     Streams in FS2 are lazy, pull-based,
     and can represent both finite and infinite sequences of data.
     These streams are designed to be resource-safe,
     ensuring that resources like files, sockets, etc.,
      are acquired and released correctly.
TODO
    Lazy Evaluation:
     Both Scala's standard library streams (scala.collection.immutable.Stream)
     and FS2 streams are lazy, meaning
     they only compute values when necessary.
      However, the similarity mostly ends there.
TODO
     Effectfulness:
     FS2 streams operate with an effect type,
     which allows them to represent asynchronous computations,
     side-effects, or other kinds of effectful operations in a controlled manner.
     Scala's standard streams don't have this concept;
     they are purely synchronous
     and don't handle effects in the same structured way.
   */

  object Model {
    case class Actor(id: Int, firstName: String, lastName: String)
  }
//todo it represents database
  object Data {
    // Justice League
    val henryCavil: Actor = Actor(0, "Henry", "Cavill")
    val galGodot: Actor = Actor(1, "Gal", "Godot")
    val ezraMiller: Actor = Actor(2, "Ezra", "Miller")
    val benFisher: Actor = Actor(3, "Ben", "Fisher")
    val rayHardy: Actor = Actor(4, "Ray", "Hardy")
    val jasonMomoa: Actor = Actor(5, "Jason", "Momoa")

    // Avengers
    val scarlettJohansson: Actor = Actor(6, "Scarlett", "Johansson")
    val robertDowneyJr: Actor = Actor(7, "Robert", "Downey Jr.")
    val chrisEvans: Actor = Actor(8, "Chris", "Evans")
    val markRuffalo: Actor = Actor(9, "Mark", "Ruffalo")
    val chrisHemsworth: Actor = Actor(10, "Chris", "Hemsworth")
    val jeremyRenner: Actor = Actor(11, "Jeremy", "Renner")
    val tomHolland: Actor = Actor(13, "Tom", "Holland")
    val tobeyMaguire: Actor = Actor(14, "Tobey", "Maguire")
    val andrewGarfield: Actor = Actor(15, "Andrew", "Garfield")
  }


//examples of FS2 streams
  //pure streams which store natural Data i.e original Scala Streams
  // but scala streams are no Longer called Streams they are called Lazy List
 // streams are abstraction to manage an unbounded amount of data
  import Data._
  val jlActors: Stream[Pure, Actor] = Stream(
    henryCavil,
    galGodot,
    ezraMiller,
    benFisher,
    rayHardy,
    jasonMomoa

  )
  val tomHollandStream: Stream[Pure, Actor] = Stream.emit(tomHolland)

  val spiderMen: Stream[Pure, Actor] = Stream.emits(List(
    tomHolland,
    tobeyMaguire,
    andrewGarfield
  ))//IO describes any kind of Computation that might perform side effects

  /*
  As no effect is used at all,
   it’s possible to convert pure streams
   into Scala List of Vector using the convenient methods provided by the library:
   */
  val jlActorList: List[Actor] = jlActors.toList
  val jlActorVector: Vector[Actor] = jlActors.toVector



  /*
  It’s also possible to create also an infinite stream.
  The fs2 library provides some convenient methods to do this:
   */
  implicit class  IODebugOps[A](io:IO[A]){
    def debug:IO[A]= io.map{
      a=>
        println(s"[${Thread.currentThread().getName}] $a")
        a
    }
  }
  /*
  The repeat method does what its name suggests;
  it repeats the stream infinitely.
  Since we cannot put an infinite stream into a list,
  we take the stream’s first n elements
  and convert them into a list as we’ve done before
   */
  val infiniteJLActors: Stream[Pure, Actor] = jlActors.repeat
  val repeatedJLActorsList: List[Actor] = infiniteJLActors.take(12).toList
  //Lets talk about Effectual Stream
  /*
  In most cases, we want to create a stream directly
   evaluating some statements that may produce side effects.
   So, for example, let’s try to persist an actor through a stream:
   */
  val liftedJlActors: Stream[IO, Actor] = Stream.eval{
    IO{
      println(s"Saving actor $tomHolland")
      Thread.sleep(1000)
      println("Finished")
      tomHolland
    }

  }
  /**
   TODO
   The fs2 library gives us the method eval that takes
   an IO effect and returns a Stream that will evaluate
  the IO effect when pulled.
  A question arises:
  How do we pull the values from an effectful stream?
  We cannot convert such a stream into a Scala collection
  using the toList function, and if we try,
  the compiler soundly yells at us:
   */

  val compiledStream: IO[Unit] = liftedJlActors.compile.drain
  val jlActorsEffectfulList: IO[List[Actor]] = liftedJlActors.compile.toList

  /**
  Chunks
          Inside, every stream is made of chunks.
           A Chunk[O] is a finite sequence of stream elements of type O
           stored inside a structure optimized
           for indexed based lookup of elements.
           We can create a stream directly through the Stream.chunk
           method, which accepts a sequence of
   */
/*
TODO
        In FS2, the concept of a Chunk is fundamental.
        A Chunk is a sequence of values
        that can be used as a building block for streams.
        It provides a way to handle
        multiple elements at once in an efficient manner.
        Let's dive deeper into the details:
TODO
      Basics of Chunk
        A Chunk in FS2 is an immutable, fast,
        and memory-efficient sequence of values.
        Unlike a generic collection in the Scala standard library,
        Chunk is specialized for the needs of FS2,
        balancing both performance and generality.
TODO
         Here are some features and characteristics of Chunk:

           Specialized Representations:
           Chunk can have specialized representations
           for certain types of data to optimize performance.
           For instance, there are special Chunk types
           like Chunk.Bytes for a sequence of bytes and Chunk.Boxed for boxed values.

             Efficient Concatenation:
             Chunks can be concatenated efficiently.
            This is crucial because when manipulating FS2 streams,
            you're often combining or splitting chunks of data.

        Indexed Access: Chunks support fast indexed access,
        allowing for efficient operations like fold, map, and filter.

Usage
You can create a Chunk from a variety of sources:


   Role in FS2 Streams
   Chunks play a vital role in the performance and efficiency of FS2 streams:

     Batching:
     Instead of handling one item at a time, like Scala Stream does
     FS2 streams operate on Chunks of data,
     allowing for batch processing.
     This can lead to much better performance,
     especially in IO-bound or CPU-bound scenarios.

     Resource Efficiency:
       By using specialized representations,
       FS2 can handle data more efficiently,
       reducing memory overhead and improving throughput.

      Stream Manipulations:
      Many FS2 operations that manipulate streams,
      like map, filter, or flatMap, work at the chunk level.
      For instance, when you map over an FS2 stream,
      you're often mapping over entire chunks at once,
      rather than individual elements.

Summary
        In essence, the Chunk type in FS2 is a low-level,
        efficient, and specialized collection that acts as the backbone for stream processing.
        It's one of the reasons FS2 can offer high performance and resource efficiency,
       especially in comparison to naive, element-by-element processing approaches.
      When working with FS2, it's useful to understand the role of Chunk,
     even if many of the common operations abstract away the chunk-level details.






 */

  import fs2._

  val fromValues: Chunk[Int] = Chunk(1, 2, 3, 4)                // From individual values
  val fromArray: Chunk[Int] = Chunk.array(Array(1, 2, 3, 4))   // From an array
  val fromSeq: Chunk[Int] = Chunk.seq(Seq(1, 2, 3, 4))         // From a sequence



  val avengersActors: Stream[Pure, Actor] = Stream.chunk(Chunk.array(Array(
    scarlettJohansson,
    robertDowneyJr,
    chrisEvans,
    markRuffalo,
    chrisHemsworth,
    jeremyRenner
  )))


  //transformation:

  /**
  TODO
           Once we’ve built a Stream,
           we can transform its values more or less
           as we make in regular Scala collections.
          For example, let’s create a stream containing all the actors
         whose hero belongs from the Justice League or the Avengers.
         We can use the ++ operator, which concatenates two streams:
   */

  // we can read like this this stream will produce the elements from jlActors andThen avengersActors
  val dcAndMarvelSuperheroes: Stream[Pure, Actor] = jlActors ++ avengersActors

  /**
      The Stream type forms a monad on the O type parameter,
      which means that a flatMap method is available on streams,
      and we can use it to concatenate operations concerning the output values of the stream.
   */

  val printedJlActors: Stream[IO, Unit] = jlActors.flatMap { actor =>
    Stream.eval(IO.println(actor))
  }

  /*
      The pattern of calling the function Stream.eval
       inside a flatMap is so common that
       fs2 provides a shortcut for it through the evalMap method:
   */
  val evalMappedJlActors: Stream[IO, Unit] = jlActors.evalMap(IO.println)

/*
If we need to perform some effects on the stream,
but we don’t want to change the type of the stream, we can use the evalTap method:
 */
val evalTappedJlActors: Stream[IO, Actor] = jlActors.evalTap(IO.println)

  override def run: IO[Unit] ={
    //IO(repeatedJLActorsList).debug.void
    compiledStream
  }

}
