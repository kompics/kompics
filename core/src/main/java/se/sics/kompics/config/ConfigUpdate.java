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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

/**
 *
 * @author lkroll
 */
public class ConfigUpdate implements Iterable<Entry<String, ConfigValue>> {

    final HashMap<String, ConfigValue> updates;
    final long versionId;
    final UUID creator;

    ConfigUpdate(HashMap<String, ConfigValue> updates, long versionId, UUID creator) {
        this.updates = updates;
        this.versionId = versionId;
        this.creator = creator;
    }

    @Override
    public Iterator<Entry<String, ConfigValue>> iterator() {
        return updates.entrySet().iterator();
    }

    public ConfigUpdateFactory modify(UUID creator) {
        return new Factory(creator);
    }
    
    public class Factory implements ConfigUpdateFactory {

        private final HashMap<String, ConfigValue> updates = new HashMap<>();
        private final UUID creator;

        private Factory(UUID creator) {
            this.creator = creator;
        }

        @Override
        public void include(String key, ConfigValue original) {
            updates.put(key, original);
        }

        @Override
        public void replace(String key, ConfigValue original, Object replacement) {
            ConfigValue newCV = Config.Builder.CVFactory.INSTANCE.create(replacement, original.version(), original.options());
            updates.put(key, newCV);
        }

        @Override
        public void replace(String key, ConfigValue original, Object replacement, ValueOptions options) {
            ConfigValue newCV = Config.Builder.CVFactory.INSTANCE.create(replacement, original.version(), options);
            updates.put(key, newCV);
        }

        @Override
        public ConfigUpdate assemble() {
            return new ConfigUpdate(this.updates, ConfigUpdate.this.versionId, this.creator);
        }
    }
}
