import java.util.concurrent.Executors

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.BalancingPool
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext

class PingTestActor extends Actor {
  def receive = {
    case (messageSendTime: Long, msg: String) => {
      println(s"Request delay ${System.currentTimeMillis() - messageSendTime}")
      sender ! Tuple2(System.currentTimeMillis(), msg)
    }
    case m @ _ => {
      //println(s"Ping ${m.getClass} ${m.asInstanceOf[String].size}")
      sender ! Tuple2(System.currentTimeMillis(), m)
    }
  }
}

class PrintActor extends Actor {
  def receive = {
    case m@ _       => println(m)
  }
}

object Main extends App {

  val count = ConfigFactory.load().getConfig("test-server").getInt("no-of-threads")

  val system = ActorSystem("ServerTestActorSystem", None, None,
    Some(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(count))))
  val pingTestActor = system.actorOf(
    Props[PingTestActor].withRouter(BalancingPool(count)), name = "PingTestActor")
  val printActor = system.actorOf(Props[PrintActor], name = "PrintActor")

  pingTestActor.tell("Hi", printActor)
}