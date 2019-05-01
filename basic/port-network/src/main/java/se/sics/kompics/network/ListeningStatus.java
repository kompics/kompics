/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;

import se.sics.kompics.Direct;
import se.sics.kompics.KompicsEvent;

/**
 * Indicates a status change or report of the listening sockets.
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class ListeningStatus implements KompicsEvent {
    public final ImmutableMap<Transport, Integer> openSockets;
    public final InetAddress boundInterface;
    public final InetAddress publicInterface;

    public ListeningStatus(ImmutableMap<Transport, Integer> openSockets, InetAddress boundInterface,
            InetAddress publicInterface) {
        this.openSockets = openSockets;
        this.boundInterface = boundInterface;
        this.publicInterface = publicInterface;
    }

    public ListeningStatus(Map<Transport, Integer> openSockets, InetAddress boundInterface,
            InetAddress publicInterface) {
        this.openSockets = ImmutableMap.copyOf(openSockets);
        this.boundInterface = boundInterface;
        this.publicInterface = publicInterface;
    }

    /**
     * The port where the current listening socket for the given protocol is bound.
     * 
     * @param proto
     *            the protocol of the socket
     * @return the current listening socket for that protocol or {@code null} if none is bound
     */
    public int getPort(Transport proto) {
        return openSockets.get(proto);
    }

    /**
     * The port where the current listening socket for the given protocol is bound, if any.
     * 
     * @param proto
     *            the protocol of the socket
     * @return the current listening socket for that protocol or {@code Optional.absent()} if none is bound
     */
    public Optional<Integer> checkPort(Transport proto) {
        if (openSockets.containsKey(proto)) {
            return Optional.of(openSockets.get(proto));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get a socket address for the public interface and the given transport protocol.
     * 
     * @param proto
     *            the transport to use
     * @return the socket address
     */
    public Optional<InetSocketAddress> socket(Transport proto) {
        return checkPort(proto).map((port) -> new InetSocketAddress(publicInterface, port));
    }

    /**
     * Get an address for the public interface and the given transport protocol.
     * 
     * @param <A>
     *            the address type to use
     * @param proto
     *            the transport to use
     * @param f
     *            a function mapping an {@code InetSocketAddress} to an instance of {@code A}.
     * @return the created instance of {@code A}.
     */
    public <A extends Address> Optional<A> address(Transport proto, Function<InetSocketAddress, A> f) {
        return socket(proto).map(f);
    }

    /**
     * Convert this into an instance of {@code Response}.
     * 
     * @return A {@code Response} containing {@code this}.
     */
    public Response asResponse() {
        return new Response(this);
    }

    /**
     * 
     * @return a new {@code Request}
     */
    public static Request request() {
        return new Request();
    }

    /**
     * Ask a network component to send the current status of the listening sockets.
     *
     */
    public static class Request extends Direct.Request<Response> {

    }

    /**
     * A direct reply to a status request.
     *
     */
    public static class Response implements Direct.Response {
        public final ListeningStatus status;

        private Response(ListeningStatus status) {
            this.status = status;
        }
    }
}
