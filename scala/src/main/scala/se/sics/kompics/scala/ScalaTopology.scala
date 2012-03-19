/**
 *
 */
package se.sics.kompics.scala

import se.sics.kompics.launch.Topology
import se.sics.kompics.address.Address
import scala.collection.JavaConverters._

/**
 * @author sario
 *
 */

class ScalaTopology(original: Topology) {
	def neighbours(node: Address) : Set[Address] = {		
			val neighs = original.getNeighbors(node);
			return neighs.asScala.toSet;
	}
}

object ScalaTopology {
	implicit def topology2scala(x: Topology): ScalaTopology = new ScalaTopology(x);
}