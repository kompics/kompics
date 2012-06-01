package se.sics.kompics.scala

import se.sics.kompics.ChannelCore
import se.sics.kompics.PortType

/**
 * The <code>ClosureChannel</code> class.
 * 
 * @author Lars Kroll <lkr@lars-kroll.com>
 * @version $Id: $
 */
class ClosureChannel[P <: PortType](val positivePort: PositivePort[P], 
		val negativePort: NegativePort[P], val portType: P) 
		extends ChannelCore[P](positivePort, negativePort, portType) {

}