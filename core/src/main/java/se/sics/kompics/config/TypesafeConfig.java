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

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class TypesafeConfig implements BaselineConfig {

    private final com.typesafe.config.Config config;

    public static Config load() {
        com.typesafe.config.Config conf = ConfigFactory.load();
        return load(conf);
    }

    public static Config load(com.typesafe.config.Config conf) {
        TypesafeConfig instance = new TypesafeConfig(conf);
        return Config.Factory.load(instance);
    }

    private TypesafeConfig(com.typesafe.config.Config config) {
        this.config = config;
    }

    @Override
    public ConfigValue getValue(String key) {
        try {
            com.typesafe.config.ConfigValue cv = config.getValue(key);
            return new TypesafeValue(cv);
        } catch (ConfigException.Missing ex) {
            return null;
        }
    }

    @Override
    public List<? extends ConfigValue> getValues(String path) {
        ConfigList cl = config.getList(path);
        if (cl != null) {
            List<ConfigValue> l = new LinkedList<>();
            for (com.typesafe.config.ConfigValue cv : cl) {
                l.add(new TypesafeValue(cv));
            }
            return l;
        } else {
            return null;
        }
    }

    private static class TypesafeValue implements ConfigValue {

        public final com.typesafe.config.ConfigValue cv;

        private TypesafeValue(com.typesafe.config.ConfigValue cv) {
            this.cv = cv;
        }

        @Override
        public Object unwrap() {
            return cv.unwrapped();
        }

        @Override
        public Class<?> type() {
            return cv.unwrapped().getClass();
        }

        @Override
        public long version() {
            return 0; // this is always a baseline value
        }

        @Override
        public ValueOptions options() {
            return ValueOptions.DEFAULT;
        }

    }
}
