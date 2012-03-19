package se.sics.kompics.scala.propose

import se.sics.kompics.timer.Timeout
import se.sics.kompics.timer.ScheduleTimeout

case class Continue(val timeout: ScheduleTimeout) extends Timeout(timeout) {

}