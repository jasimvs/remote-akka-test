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

  val numberOfConcurrentRequests = 1024
  val numberOfTestCycles = 1

  val testMessage = (1 to 127000)
    .map(x => "x")
    .fold("")((acc, str) => acc + str)

  val system = ActorSystem("ClientTestActorSystem", None, None,
    Some(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(numberOfConcurrentRequests))))

  val printActor = system.actorOf(
      Props[PrintActor].withRouter(BalancingPool(numberOfConcurrentRequests)), name = "PrintActor")

  val remoteActor = system.actorSelection("akka.tcp://ServerTestActorSystem@127.0.0.1:2552/user/PingTestActor")

  val sleepFactor = 3


  // warm up
  1 to numberOfConcurrentRequests map (x => {
    remoteActor.tell(testMessage, printActor)
  })
  println("Warming up...")
  Thread.sleep(2000 + numberOfConcurrentRequests * sleepFactor)


  // Test
  1 to numberOfTestCycles map (i => {

    TimestampLog.timestamps.clear()
    val startTime = System.currentTimeMillis()

    1 to numberOfConcurrentRequests map (x => {
      remoteActor.tell((System.currentTimeMillis(), testMessage), printActor)
      //println(x)
    })
    println(s"Total time to sent concurrent requests ${System.currentTimeMillis() - startTime}")
    Thread.sleep(2000 + numberOfConcurrentRequests * sleepFactor)


    // Results
    val sorted = TimestampLog.timestamps.sortWith(_._1 < _._1)
    sorted.map(x => println(s"Response delay: ${x._2} , Total time: ${x._1 - startTime}"))
    println(
      s" Total Time: ${sorted.reverse.head._1 - startTime} millis for ${TimestampLog.timestamps.size} messages. Test $i")
  })
}