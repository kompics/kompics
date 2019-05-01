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
package se.sics.kompics.network.netty;

import com.google.common.base.Optional;
import se.sics.kompics.network.MessageNotify;
import se.sics.kompics.network.Msg;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
class MessageWrapper {

    public final Msg<?, ?> msg;
    public final Optional<MessageNotify.Req> notify;

    MessageWrapper(MessageNotify.Req notify) {
        this.msg = notify.msg;
        this.notify = Optional.of(notify);
    }

    MessageWrapper(Msg<?, ?> msg) {
        this.msg = msg;
        this.notify = Optional.absent();
    }

    void injectSize(int diff, long startTS) {
        if (notify.isPresent()) {
            notify.get().injectSize(diff, startTS);
        }
    }

    @Override
    public String toString() {
        if (notify.isPresent()) {
            return "MessageWrapper(" + notify.get() + ")";
        } else {
            return "MessageWrapper(" + msg + ")";
        }
    }
}
