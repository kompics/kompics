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
package se.sics.kompics.config;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class ValueOptions {

    public static final ValueOptions DEFAULT = new ValueOptions();

    public static enum Copy {

        SHALLOW, DEEP;
    }

    public final Copy copy;
    public final Cloner cloner;
    public final ValueMerger merger;

    private ValueOptions() {
        this(null, ValueMerger.HIGHEST_ID);
    }

    private ValueOptions(Cloner c) {
        this(c, ValueMerger.HIGHEST_ID);
    }

    private ValueOptions(Cloner c, ValueMerger vm) {
        if (c == null) {
            this.copy = Copy.SHALLOW;
        } else {
            this.copy = Copy.DEEP;
        }
        this.cloner = c;
        this.merger = vm;
    }

    public static ValueOptions deepCopy(Cloner cloner) {
        return new ValueOptions(cloner);
    }

    public ValueOptions withCloner(Cloner cloner) {
        return new ValueOptions(cloner, merger);
    }

    public static ValueOptions usingMerger(ValueMerger vm) {
        return new ValueOptions(null, vm);
    }

    public ValueOptions withMerger(ValueMerger vm) {
        return new ValueOptions(cloner, vm);
    }
}
