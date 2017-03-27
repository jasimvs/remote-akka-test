import java.util.concurrent.Executors

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.BalancingPool
import com.typesafe.config.ConfigFactory

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object TimestampLog {

  type ReceiveTimeAndDelayTuple = (Long, Long)

  val timestamps: mutable.MutableList[ReceiveTimeAndDelayTuple] = mutable.MutableList.empty

  def insert(value: ReceiveTimeAndDelayTuple) = timestamps += value
}

class PrintActor extends Actor {

  def receive = {
    case (messageSendTime: Long, msg: String) => {
      val currentTime = System.currentTimeMillis()
      Future(TimestampLog.insert((currentTime, currentTime - messageSendTime)))(Main.ecForLoggingTimestamps)
    }
    case msg @ _ => println(s"Ignoring $msg")
  }
}

object Main extends App {

  val conf = ConfigFactory.load().getConfig("test-client")
  val numberOfConcurrentRequests = conf.getInt("no-of-concurrent-requests")
  val numberOfTestCycles = conf.getInt("no-of-test-cycles")
  val testMessageStringSize = conf.getInt("test-message-string-size")
  val hostname = conf.getString("remote-server-hostname-port")
  val waitFactor = conf.getInt("factor-to-wait-for-requests-to-complete")
  val waitTime = numberOfConcurrentRequests * waitFactor
  val testMessage = (1 to testMessageStringSize)
    .map(x => "x")
    .fold("")((acc, str) => acc + str)

  val system = ActorSystem("ClientTestActorSystem", None, None,
    Some(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(numberOfConcurrentRequests))))
  val printActor = system.actorOf(
      Props[PrintActor].withRouter(BalancingPool(numberOfConcurrentRequests)), name = "PrintActor")
  val remoteActor = system.actorSelection(s"akka.tcp://ServerTestActorSystem@$hostname/user/PingTestActor")

  val ecForLoggingTimestamps = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())


  // warm up
  1 to numberOfConcurrentRequests map (x => {
    remoteActor.tell(testMessage, printActor)
  })
  println("Warming up...")
  Thread.sleep(1000 + waitTime)


  // Test
  1 to numberOfTestCycles map (i => {

    TimestampLog.timestamps.clear()
    val startTime = System.currentTimeMillis()

    1 to numberOfConcurrentRequests map (x => {
      remoteActor.tell((System.currentTimeMillis(), testMessage), printActor)
      //println(x)
    })
    println(s"Total time to sent concurrent requests ${System.currentTimeMillis() - startTime}")
    Thread.sleep(waitTime)

    while(TimestampLog.timestamps.isEmpty) {
      Thread.sleep(1000)
    }

    // Results
    val sorted = TimestampLog.timestamps.sortWith(_._1 < _._1)
    sorted.map(x => println(s"Response delay: ${x._2} , Total time: ${x._1 - startTime}"))
    println(
      s" Total Time: ${sorted.reverse.head._1 - startTime} millis for ${TimestampLog.timestamps.size} messages. Test $i")
  })

  Await.ready(system.terminate(), Duration.Inf)
}