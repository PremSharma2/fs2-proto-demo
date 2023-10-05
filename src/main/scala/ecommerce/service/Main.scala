package ecommerce.service
import cats.effect._
import com.ecom.protos.inventory.{NotifyRequest, ProductUpdate}
import fs2.concurrent.Topic
import cats.effect._
import cats.syntax.all._
import fs2.concurrent.Topic
/*
object Main extends IOApp.Simple {
/*
  override  def run(args: List[String]): IO[ExitCode] = {
    for {
      topic <- Topic[IO, ProductUpdate](ProductUpdate("", 0))
      inventoryService = new InventoryService(topic)
      notificationService = new NotificationService(topic)

      // Simulating adding a user to the notify list for a product
      _ <- notificationService.addToNotifyList(NotifyRequest("product123", "user456"))

      // Simulating the product coming back into stock
      _ <- inventoryService.updateInventory(ProductUpdate("product123", 10))

      // Start the notifier stream (this will run indefinitely)
      _ <- notificationService.startNotifierStream().compile.drain
    } yield ExitCode.Success
  }


 */
}
