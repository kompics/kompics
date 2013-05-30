/**
 * This file is part of the Kompics P2P Framework.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.p2p.monitor.chord.client;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.Transport;
import se.sics.kompics.p2p.monitor.chord.server.ChordNeighborsNotification;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsRequest;
import se.sics.kompics.p2p.overlay.chord.ChordNeighborsResponse;
import se.sics.kompics.p2p.overlay.chord.ChordStatus;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>ChordMonitorClient</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class ChordMonitorClient extends ComponentDefinition {

    Positive<ChordStatus> chordStatus = positive(ChordStatus.class);
    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    private Logger logger;
    private UUID sendViewTimeoutId;
    private Address monitorServerAddress;
    private Address self;
    private int clientWebPort;
    private long updatePeriod;
    private Transport protocol;

    public ChordMonitorClient(ChordMonitorClientInit init) {

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        subscribe(handleChangeUpdatePeriod, network);
        subscribe(handleGetChordNeighborsResponse, chordStatus);
        subscribe(handleSendView, timer);

        // INIT
        self = init.getSelf();
        updatePeriod = init.getConfiguration().getClientUpdatePeriod();
        monitorServerAddress = init.getConfiguration()
                .getMonitorServerAddress();
        clientWebPort = init.getConfiguration().getClientWebPort();
        protocol = init.getConfiguration().getProtocol();

        logger = LoggerFactory.getLogger(getClass().getName() + "@"
                + self.getId());
    }
    private Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
                    updatePeriod, updatePeriod);
            spt.setTimeoutEvent(new SendView(spt));
            sendViewTimeoutId = spt.getTimeoutEvent().getTimeoutId();

            trigger(spt, timer);
        }
    };
    private Handler<Stop> handleStop = new Handler<Stop>() {
        public void handle(Stop event) {
            trigger(new CancelPeriodicTimeout(sendViewTimeoutId), timer);
        }
    };
    private Handler<ChangeUpdatePeriod> handleChangeUpdatePeriod = new Handler<ChangeUpdatePeriod>() {
        public void handle(ChangeUpdatePeriod event) {
            updatePeriod = event.getNewUpdatePeriod();
            trigger(new CancelPeriodicTimeout(sendViewTimeoutId), timer);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(
                    updatePeriod, updatePeriod);
            spt.setTimeoutEvent(new SendView(spt));
            sendViewTimeoutId = spt.getTimeoutEvent().getTimeoutId();
            trigger(spt, timer);
        }
    };
    private Handler<SendView> handleSendView = new Handler<SendView>() {
        public void handle(SendView event) {
            logger.debug("SEND_VIEW");

            ChordNeighborsRequest request = new ChordNeighborsRequest();
            trigger(request, chordStatus);
        }
    };
    private Handler<ChordNeighborsResponse> handleGetChordNeighborsResponse = new Handler<ChordNeighborsResponse>() {
        public void handle(ChordNeighborsResponse event) {
            logger.debug("GET_CHORD_NEIGHBORS_RESP");

            if (event.getNeighbors().getLocalPeer() != null) {
                // only send notification to the server if the peer has joined
                ChordNeighborsNotification viewNotification = new ChordNeighborsNotification(
                        self, monitorServerAddress, protocol, clientWebPort,
                        event.getNeighbors());

                trigger(viewNotification, network);
            }
        }
    };
}
