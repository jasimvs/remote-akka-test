import java.util.concurrent.Executors

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.BalancingPool

import scala.concurrent.ExecutionContext

class PingTestActor extends Actor {
  def receive = {
    case m@ _       => {
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

  val count = 1024

  val system = ActorSystem("ServerTestActorSystem", None, None,
    Some(ExecutionContext.fromExecutor(Executors.newFixedThreadPool(count))))
  // default Actor constructor
  val pingTestActor = system.actorOf(Props[PingTestActor].withRouter(BalancingPool(count)), name = "PingTestActor")
  val printActor = system.actorOf(Props[PrintActor], name = "PrintActor")

  pingTestActor.tell("Hi", printActor)
}