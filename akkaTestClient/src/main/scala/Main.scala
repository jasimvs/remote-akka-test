import java.util.concurrent.Executors

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.BalancingPool

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

object TimestampLog {

  type ReceiveTimeAndDelayTuple = (Long, Long)

  val timestamps: mutable.MutableList[ReceiveTimeAndDelayTuple] = mutable.MutableList.empty

  def insert(value: ReceiveTimeAndDelayTuple) = synchronized(timestamps += value)
}

class PrintActor extends Actor {

  implicit val exc = Main.ec

  def receive = {
    case msg: (Long, String) => {
      val currentTime = System.currentTimeMillis()
      Future(TimestampLog.insert((currentTime, currentTime - msg._1)))
    }
    case msg @ _ => println(s"Ignoring $msg")
  }
}

object Main extends App {

  val ec = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  val testMessagesCount = 1024

  val system = ActorSystem("TestActorSystem", None, None,
    Some(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(testMessagesCount))))
  val printActor = system.actorOf(Props[PrintActor].withRouter(BalancingPool(testMessagesCount)), name = "PrintActor")

  val selection =
    system.actorSelection("akka.tcp://PingTestActorSystem@127.0.0.1:2552/user/PingTestActor")

  val testBigMessage = (1 to 27500).fold("": String)((a, x) => a + x.toString)

  val sleepFactor = 3

  // warm up
  1 to testMessagesCount map (x => {
    selection.tell(testBigMessage, printActor)
  })
  println("Warming up...")
  Thread.sleep(2000 + testMessagesCount * sleepFactor)

  1 to 10 map (i => {

    TimestampLog.timestamps.clear()

    val startTime = System.currentTimeMillis()

    1 to testMessagesCount map (x => {
      selection.tell(testBigMessage, printActor)
      //println(x)
    })
    Thread.sleep(2000 + testMessagesCount * sleepFactor)

    val sorted = TimestampLog.timestamps.sortWith(_._1 < _._1)

    //sorted.map(x => println(s"Delay: ${x._2}  Time to receive msg: ${x._1 - startTime}"))

    println(
      s" Total Time: ${sorted.reverse.head._1 - startTime} millis for ${TimestampLog.timestamps.size} messages. Test $i")
  })
}