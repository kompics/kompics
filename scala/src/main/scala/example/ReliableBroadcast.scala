/**
 *
 */
package se.sics.kompics.scala.propose

import se.sics.kompics.scala.ComponentDefinition
import se.sics.kompics.address.Address
import se.sics.kompics.launch.Topology
import scala.collection.JavaConverters._
import se.sics.kompics.Event
import se.kth.ict.id2203.pp2p.PerfectPointToPointLink
import se.sics.kompics.scala.ScalaTopology._
import se.sics.kompics.scala.NegativePort
import se.sics.kompics.scala.PositivePort
import se.sics.kompics.scala.Init

/**
 * @author sario
 *
 */
class ReliableBroadcast extends ComponentDefinition {

	val reb = ++(SimpleBroadcast);
	val pp2p = --(classOf[PerfectPointToPointLink]);
	
	private var topology : Topology = null;
	
	def self : Address = topology.getSelfAddress();
	def neighbours : Set[Address] = topology.neighbours(self);

	ctrl uponEvent {case Init(topology: Topology) => {() =>
		this.topology = topology;
	}}
	
	reb uponEvent {case SBSend(msg) => {() => 
		neighbours foreach {
			q => trigger(Pp2pSend(q, msg), pp2p)
		}
		}}
	
	pp2p uponEvent {case Pp2pDeliver(src: Address, m: Any) => {() =>
		trigger(SBDeliver(src, m), reb)
	}}
}