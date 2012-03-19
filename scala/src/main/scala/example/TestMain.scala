/**
 *
 */
package se.sics.kompics.scala.propose

import se.sics.kompics.scala.ComponentDefinition
import se.sics.kompics.launch.Topology
import se.sics.kompics.timer.java.JavaTimer
import se.sics.kompics.network.mina.MinaNetwork
import se.kth.ict.id2203.pp2p.delay.DelayLink
import se.sics.kompics.Kompics
import org.apache.log4j.PropertyConfigurator
import se.sics.kompics.Component
import se.sics.kompics.address.Address
import se.sics.kompics.Event
import se.sics.kompics.scala.DSLComponent
import se.sics.kompics.timer.Timer
import se.sics.kompics.PortType
import se.sics.kompics.scala.ScalaTopology._
import se.sics.kompics.scala.Init
import se.sics.kompics.network.Network
import se.kth.ict.id2203.pp2p.PerfectPointToPointLink

/**
 * @author sario
 *
 */
class TestMain extends ComponentDefinition {
	val topology = Topology.load(System.getProperty("topology"), TestMain.selfId);
	val self: Address = topology.getSelfAddress();
	def neighbours : Set[Address] = topology.neighbours(self);
	
	val time = init(Init(), classOf[JavaTimer]);
	val network = init(Init(self, 5), classOf[MinaNetwork]);
	val pp2p = init(Init(topology), classOf[DelayLink]);
	val reb = init(Init(topology), classOf[ReliableBroadcast]);
	val app = init(Init(TestMain.commandScript, neighbours, self), classOf[TestApplication]);
	
	pp2p -- classOf[Network] ++ network;
	reb -- classOf[PerfectPointToPointLink] ++ pp2p;
	app -- SimpleBroadcast ++ reb;
	time ++ classOf[Timer] -- (app, pp2p);
	
	
	
	
//	connect(app, reb);
//	connect(app, time);
//	connect(reb, pp2p);
//	connect(pp2p, time);
//	connect(pp2p, network);
	
	
	
}

object TestMain {
	PropertyConfigurator.configureAndWatch("log4j.properties");

	var selfId: Int = 0;
	var commandScript: String = null;

	def main(args: Array[String]) {
		selfId = Integer.parseInt(args(0));
		commandScript = args(1);
		Kompics.createAndStart(classOf[TestMain]);
	}
}