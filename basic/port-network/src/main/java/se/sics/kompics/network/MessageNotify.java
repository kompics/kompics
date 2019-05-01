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

import java.util.UUID;
import se.sics.kompics.Direct;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class MessageNotify {

    public static enum State {

        IN_PROGRESS, // internal only
        SENT, DELIVERED, FAILED;
    }

    public static Req create(Msg<?, ?> msg) {
        Req req = new Req(msg);
        return req;
    }

    public static Req createWithDeliveryNotification(Msg<?, ?> msg) {
        return new Req(msg, true);
    }

    public static class Req extends Direct.Request<Resp> {

        public final Msg<?, ?> msg;
        public final boolean notifyOfDelivery;

        public Req(Msg<?, ?> msg) {
            this(msg, false);
        }

        public Req(Msg<?, ?> msg, boolean notifyOfDelivery) {
            this.msg = msg;
            this.notifyOfDelivery = notifyOfDelivery;
            this.setResponse(new Resp());
        }

        public UUID getMsgId() {
            return this.getResponse().msgId;
        }

        public void prepareResponse(long time, boolean success, long nanoEnd) {
            this.getResponse().setTime(time);
            this.getResponse().setState(success ? State.SENT : State.FAILED);
            this.getResponse().setSendTime(nanoEnd - this.getResponse().getSendTime());
        }

        public void injectSize(int size, long nanoStart) {
            this.getResponse().setSize(size);
            this.getResponse().setSendTime(nanoStart);
            this.getResponse().setDeliveryTime(nanoStart);
        }

        public Resp deliveryResponse(long time, boolean success, long nanoEnd) {
            try {
                Resp resp = this.getResponse().clone();
                resp.setTime(time);
                resp.setState(success ? State.DELIVERED : State.FAILED);
                resp.setDeliveryTime(nanoEnd - resp.getDeliveryTime());
                return resp;
            } catch (CloneNotSupportedException ex) {
                throw new RuntimeException(ex); // shouldn't be thrown in the first place
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MessageNotify.Req(");
            sb.append("notifyOfDelivery=");
            sb.append(this.notifyOfDelivery);
            sb.append(", msg=\n    ");
            sb.append(msg);
            sb.append("\n, resp=\n    ");
            this.getResponse().appendTo(sb);
            sb.append("\n)");
            return sb.toString();
        }
    }

    public static class Resp implements Direct.Response, Cloneable {

        private long time;
        private long sendTime;
        private long deliveryTime;
        private int size;
        public final UUID msgId;
        private State state = State.IN_PROGRESS;

        public Resp() {
            msgId = UUID.randomUUID();
        }

        void setTime(long time) {
            this.time = time;
        }

        /**
         *
         * @return the time when then data was sent in ms
         */
        public long getTime() {
            return time;
        }

        void setState(State state) {
            this.state = state;
        }

        public boolean isSuccess() {
            return (state == State.SENT) || (state == State.DELIVERED);
        }

        public State getState() {
            return state;
        }

        void setSize(int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        /**
         * @return the time it took to send the data over the wire in ns
         */
        public long getSendTime() {
            return sendTime;
        }

        void setSendTime(long sendTime) {
            this.sendTime = sendTime;
        }

        public long getDeliveryTime() {
            return this.deliveryTime;
        }

        void setDeliveryTime(long deliveryTime) {
            this.deliveryTime = deliveryTime;
        }

        @Override
        public Resp clone() throws CloneNotSupportedException {
            Resp that = (Resp) super.clone();
            that.time = this.time;
            that.sendTime = this.sendTime;
            that.deliveryTime = this.deliveryTime;
            that.size = this.size;
            that.state = this.state;
            return that;
        }

        private void appendTo(StringBuilder sb) {
            sb.append("MessageNotify.Resp(");
            sb.append("id=");
            sb.append(this.msgId);
            sb.append(", time=");
            sb.append(this.time);
            sb.append("ms, sendTime=");
            sb.append(this.sendTime);
            sb.append("ns, deliveryTime=");
            sb.append(this.deliveryTime);
            sb.append("ns, size=");
            sb.append(this.size);
            sb.append("bytes, state=");
            sb.append(this.state);
            sb.append(")");
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            appendTo(sb);
            return sb.toString();
        }
    }
}
