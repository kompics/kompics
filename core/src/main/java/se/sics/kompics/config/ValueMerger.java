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

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;


/**
 *
 * @author lkroll
 */
public interface ValueMerger {
    public ConfigValue merge(String key, ConfigValue oldValue, ConfigValue newValue, ConfigValueFactory cvFactory);
    public static final ValueMerger NEWEST = new ValueMerger() {

        @Override
        public ConfigValue merge(String key, ConfigValue oldValue, ConfigValue newValue, ConfigValueFactory cvFactory) {
            return newValue;
        }
    };
    public static final ValueMerger HIGHEST_ID = new ValueMerger() {

        @Override
        public ConfigValue merge(String key, ConfigValue oldValue, ConfigValue newValue, ConfigValueFactory cvFactory) {
            if (oldValue.version() < newValue.version()) {
                return newValue;
            } else {
                return oldValue;
            }
        }
    };
    public static final ValueMerger APPEND = new ValueMerger() {

        @Override
        public ConfigValue merge(String key, ConfigValue oldValue, ConfigValue newValue, ConfigValueFactory cvFactory) {
            Object o = oldValue.unwrap();
            if (o instanceof Collection) {
                Collection c = (Collection) o;
                c.add(newValue.unwrap());
                return cvFactory.create(c, Math.max(oldValue.version(), newValue.version()), newValue.options());
            } else {
                ArrayList l = new ArrayList();
                l.add(oldValue.unwrap());
                l.add(newValue.unwrap());
                return cvFactory.create(l, Math.max(oldValue.version(), newValue.version()), newValue.options());
            }
        }
    };
    public static final Optional<ValueMerger> NONE = Optional.absent();
}
