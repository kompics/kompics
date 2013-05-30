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
package se.sics.kompics.p2p.monitor.cyclon.client;

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
import se.sics.kompics.p2p.monitor.cyclon.server.CyclonNeighborsNotification;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsRequest;
import se.sics.kompics.p2p.overlay.cyclon.CyclonNeighborsResponse;
import se.sics.kompics.p2p.overlay.cyclon.CyclonStatus;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

/**
 * The
 * <code>CyclonMonitorClient</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id$
 */
public class CyclonMonitorClient extends ComponentDefinition {

    Positive<CyclonStatus> cyclonStatus = positive(CyclonStatus.class);
    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    private Logger logger;
    private UUID sendViewTimeoutId;
    private Address monitorServerAddress;
    private Address self;
    private long updatePeriod;
    private Transport protocol;

    public CyclonMonitorClient(CyclonMonitorClientInit init) {

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        subscribe(handleChangeUpdatePeriod, network);
        subscribe(handleCyclonNeighborsResponse, cyclonStatus);
        subscribe(handleSendView, timer);

        // INIT
        self = init.getSelf();
        updatePeriod = init.getConfiguration().getClientUpdatePeriod();
        monitorServerAddress = init.getConfiguration()
                .getMonitorServerAddress();
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

            CyclonNeighborsRequest request = new CyclonNeighborsRequest();
            trigger(request, cyclonStatus);
        }
    };
    private Handler<CyclonNeighborsResponse> handleCyclonNeighborsResponse = new Handler<CyclonNeighborsResponse>() {
        public void handle(CyclonNeighborsResponse event) {
            logger.debug("CYCLON_NEIGHBORS_RESP");

            if (event.getNeighbors().getSelf() != null) {
                // only send notification to the server if the peer has joined
                CyclonNeighborsNotification viewNotification = new CyclonNeighborsNotification(
                        self, monitorServerAddress, protocol, event
                        .getNeighbors());

                trigger(viewNotification, network);
            }
        }
    };
}
