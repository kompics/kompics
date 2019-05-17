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
package se.sics.kompics;

import java.util.Optional;
import se.sics.kompics.config.ConfigUpdate;
import se.sics.kompics.config.ConfigUpdateFactory;
import se.sics.kompics.config.ValueMerger;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class UpdateAction {

    public interface Mapper {
        public ConfigUpdate map(ConfigUpdate original, ConfigUpdateFactory factory);
    }

    public enum Propagation {

        SWALLOW, ORIGINAL, MAP;
    }

    public static final UpdateAction DEFAULT = UpdateAction.create().finalise();

    public final Propagation upStrategy;
    public final Mapper upMapper;
    public final Propagation selfStrategy;
    public final Mapper selfMapper;
    public final Propagation downStrategy;
    public final Mapper downMapper;
    public final Optional<ValueMerger> merger;

    private UpdateAction(Propagation upStrategy, Mapper upMapper, Propagation selfStrategy, Mapper selfMapper,
            Propagation downStrategy, Mapper downMapper, Optional<ValueMerger> merger) {
        this.upStrategy = upStrategy;
        this.upMapper = upMapper;
        this.selfStrategy = selfStrategy;
        this.selfMapper = selfMapper;
        this.downStrategy = downStrategy;
        this.downMapper = downMapper;
        this.merger = merger;
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {

        private Propagation upStrategy = Propagation.ORIGINAL;
        private Mapper upMapper = null;
        private Propagation selfStrategy = Propagation.ORIGINAL;
        private Mapper selfMapper = null;
        private Propagation downStrategy = Propagation.ORIGINAL;
        private Mapper downMapper = null;
        private Optional<ValueMerger> merger = Optional.empty();

        public UpdateAction finalise() {
            return new UpdateAction(upStrategy, upMapper, selfStrategy, selfMapper, downStrategy, downMapper, merger);
        }

        public void originalUp() {
            this.upStrategy = Propagation.ORIGINAL;
            this.upMapper = null;
        }

        public void originalSelf() {
            this.selfStrategy = Propagation.ORIGINAL;
            this.selfMapper = null;
        }

        public void originalDown() {
            this.downStrategy = Propagation.ORIGINAL;
            this.downMapper = null;
        }

        public void swallowUp() {
            this.upStrategy = Propagation.SWALLOW;
            this.upMapper = null;
        }

        public void swallowSelf() {
            this.selfStrategy = Propagation.SWALLOW;
            this.selfMapper = null;
        }

        public void swallowDown() {
            this.downStrategy = Propagation.SWALLOW;
            this.downMapper = null;
        }

        public void mapUp(Mapper mapper) {
            this.upStrategy = Propagation.MAP;
            this.upMapper = mapper;
        }

        public void mapSelf(Mapper mapper) {
            this.selfStrategy = Propagation.MAP;
            this.selfMapper = mapper;
        }

        public void mapDown(Mapper mapper) {
            this.downStrategy = Propagation.MAP;
            this.downMapper = mapper;
        }

        public void customMergeWith(ValueMerger merger) {
            this.merger = Optional.ofNullable(merger);
        }
    }
}
