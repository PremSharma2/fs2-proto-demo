package ecommerce.service
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.concurrent.Topic
import com.ecom.protos.inventory._
class InventoryService(topic: Topic[IO, ProductUpdate]) {

  var inventory: Map[String, Int] = Map() // A simple in-memory representation. In real-world, use a database.

  def updateInventory(update: ProductUpdate): IO[Unit] = IO {
    inventory = inventory.updated(update.productId, update.quantity)
    if (update.quantity > 0) {
      topic.publish1(update).unsafeRunSync() // Notify if product is back in stock
    }
  }
}
