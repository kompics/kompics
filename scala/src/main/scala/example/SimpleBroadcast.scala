/**
 *
 */
package se.sics.kompics.scala.propose
import se.sics.kompics.PortType

/**
 * @author sario
 *
 */
object SimpleBroadcast extends PortType {
	request(classOf[SBSend]);
	indication(classOf[SBDeliver]);	
}