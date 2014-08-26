package se.sics.kompics;

import java.lang.ref.WeakReference;

public final class RequestPathElement implements Comparable<RequestPathElement> {

    private final WeakReference<ChannelCore<?>> channel;

    private final WeakReference<ComponentCore> component;

    private final boolean isChannel;

    public RequestPathElement(ChannelCore<?> channel) {
        super();
        this.channel = new WeakReference<ChannelCore<?>>(channel);
        this.component = null;
        this.isChannel = true;
    }

    public RequestPathElement(ComponentCore component) {
        super();
        this.channel = null;
        this.component = new WeakReference<ComponentCore>(component);
        this.isChannel = false;
    }

    public ChannelCore<?> getChannel() {
        return channel.get();
    }

    public ComponentCore getComponent() {
        return component.get();
    }

    public boolean isChannel() {
        return isChannel;
    }

    @Override
    public String toString() {
        if (isChannel) {
            return "Channel: " + channel.get();
        }
        ComponentCore c = component.get();
        return "Component: " + (c == null ? null : c.getComponent());
    }

    @Override
    public int compareTo(RequestPathElement o) {
        if (isChannel != o.isChannel) {
            return isChannel ? -1 : 1;
        }
        if (isChannel) {
            ChannelCore<?> thisChannel = channel.get();
            ChannelCore<?> oChannel = o.channel.get();
            if (thisChannel == null) {
                return (oChannel == null) ? 0 : -1;
            }
            if (oChannel == null) {
                return 1;
            }
            if (thisChannel.equals(oChannel)) {
                return 0;
            } else {
                return isChannel ? -1 : 1;
            }
        } else {
            ComponentCore thisComp = component.get();
            ComponentCore oComp = o.component.get();
            if (thisComp == null) {
                return (oComp == null) ? 0 : -1;
            }
            if (oComp == null) {
                return 1;
            }
            if (thisComp.equals(oComp)) {
                return 0;
            } else {
                return isChannel ? -1 : 1;
            }
        }
    }
}
