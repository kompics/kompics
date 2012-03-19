/**
 *
 */
package se.sics.kompics.scala

import se.sics.kompics.ChannelCore
import se.sics.kompics.PortType

/**
 * @author sario
 *
 */
class ClosureChannel[P <: PortType](val positivePort: PositivePort[P], 
		val negativePort: NegativePort[P], val portType: P) 
		extends ChannelCore[P](positivePort, negativePort, portType) {

}