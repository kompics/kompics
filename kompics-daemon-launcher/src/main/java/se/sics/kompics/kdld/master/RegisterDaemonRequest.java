package se.sics.kompics.kdld.master;

import se.sics.kompics.Event;
import se.sics.kompics.p2p.overlay.OverlayAddress;

public final class RegisterDaemonRequest extends Event {

	private final OverlayAddress overlayAddress;

	public RegisterDaemonRequest(OverlayAddress overlayAddress) {
		super();
		this.overlayAddress = overlayAddress;
	}

	public OverlayAddress getOverlayAddress() {
		return overlayAddress;
	}
}
