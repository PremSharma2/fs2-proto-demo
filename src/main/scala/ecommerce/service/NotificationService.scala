package ecommerce.service

import cats.effect.IO
import com.ecom.protos.inventory.{NotifyRequest, ProductUpdate}
import ecommerce._
import fs2.concurrent.Topic

class NotificationService(topic: Topic[IO, ProductUpdate]) {

  var notifyList: Map[String, List[String]] = Map() // Key: productId, Value: List of userIds

  def addToNotifyList(request: NotifyRequest): IO[Unit] = IO {
    val currentList = notifyList.getOrElse(request.productId, List())
    notifyList = notifyList.updated(request.productId, currentList :+ request.userId)
  }

  def startNotifierStream(): fs2.Stream[IO, Unit] = {
    topic.subscribe(10) // Subscribe to product updates
      .evalMap { update =>
        val userIds = notifyList(update.productId)
        IO {
          // Send notifications to users. This is a placeholder. In a real-world scenario, you'd perhaps send emails or push notifications.
          println(s"Product ${update.productId} is back in stock. Notifying users: $userIds")
          notifyList = notifyList - update.productId // Clear the notify list for this product
        }
      }
  }
}
