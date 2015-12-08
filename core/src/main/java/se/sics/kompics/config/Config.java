/**
 * This file is part of the Kompics component model runtime.
 * <p>
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.config;

import com.google.common.base.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author lkroll
 */
public interface Config {

    /**
     * Returns an {@code Optional} of the value at {@code key} as {@code T} or
     * {@code Absent<T>} if none.
     * <p>
     * Does not perform checked casting!
     * <p>
     * @param <T> The requested type of the value
     * @param key The location of the value
     * @return {@code Optional.of(T)} if present or {@code Absent<T>} otherwise
     */
    public <T> Optional<T> readValue(String key);

    /**
     * Returns an {@code Optional} of the value at {@code key} as {@code T} or
     * {@code Absent<T>} if none.
     * <p>
     * Performs checked casting against {@code type}.
     * <p>
     * @param <T> The requested type of the value
     * @param key The location of the value
     * @param type The type to cast the value to
     * @return {@code Optional.of(T)} if present or {@code Absent<T>} otherwise
     */
    public <T> Optional<T> readValue(String key, Class<T> type);

    /**
     * Return the value at {@code key} as {@code T} or {@code null} if none.
     * <p>
     * Performs checked casting against {@code type}.
     * <p>
     * @param <T> The requested type of the value
     * @param key The location of the value
     * @param type The type to cast the value to
     * @return The value as {@code T} if present or {@code null} otherwise
     * @throws ClassCastException if the value can not be cast to {@code type}
     */
    public <T> T getValue(String key, Class<T> type) throws ClassCastException;

    /**
     * Return the value at {@code key} as {@code T} or {@code defaultValue} if
     * none.
     * <p>
     * Performs checked casting against the type of {@code defaultValue}.
     * <p>
     * @param <T> The requested type of the value
     * @param key The location of the value
     * @param defaultValue Returned if there is not value of the right type at
     * {@code key}
     * @return The value as {@code T} if present or {@code defaultValue}
     * otherwise
     */
    public <T extends Object> T getValueOrDefault(String key, T defaultValue);

    /**
     * Returns a list of values at @{code key}.
     * <p>
     * The casts to {@code T} are unchecked in this method.
     * Use {@link #getValues(java.lang.String, java.lang.Class) } if you don't
     * want this behaviour.
     * <p>
     * @param <T> The list value type
     * @param key The location of the values
     * @return A list of values at @{code key}
     */
    public <T> List<T> getValues(String key);

    /**
     * Returns a list of values at @{code key}.
     * <p>
     * Value casts are checked against {@code type}.
     * May throw a @{link se.sics.kompics.config.ConfigValueException } if a
     * value can't be cast.
     * <p>
     * @param <T> The list value type
     * @param key The location of the values
     * @param type The type to cast the values to
     * @return A list of values at @{code key}
     */
    public <T> List<T> getValues(String key, Class<T> type);

    public Builder modify(UUID author);

    public Config copy(boolean newVersionLine);

    public static class Impl implements Config {

        private final AtomicLong versionFactory;
        private final BaselineConfig baseline;
        private final HashMap<String, ConfigValue> values = new HashMap<>();
        long version;

        private Impl(BaselineConfig baseline, long version) {
            this(baseline, version, new AtomicLong(0));
        }

        private Impl(BaselineConfig baseline, long version, AtomicLong versionFactory) {
            this.baseline = baseline;
            this.version = version;
            this.versionFactory = versionFactory;
        }

        @Override
        public <T> Optional<T> readValue(String key) {
            ConfigValue cv = values.get(key);
            if (cv != null) {
                T v = (T) cv.unwrap();
                return Optional.of(v);
            }
            cv = baseline.getValue(key);
            if (cv != null) {
                T v = (T) cv.unwrap();
                return Optional.of(v);
            }
            return Optional.absent();
        }

        @Override
        public <T> Optional<T> readValue(String key, Class<T> type) {
            ConfigValue cv = values.get(key);
            try {
                if (cv != null) {
                    T v = Conversions.convert(cv.unwrap(), type);
                    if (v != null) {
                        return Optional.of(v);
                    } else {
                        return Optional.absent();
                    }
                }
                cv = baseline.getValue(key);
                if (cv != null) {
                    T v = Conversions.convert(cv.unwrap(), type);
                    if (v != null) {
                        return Optional.of(v);
                    } else {
                        return Optional.absent();
                    }
                }
                return Optional.absent();
            } catch (ClassCastException ex) {
                return Optional.absent();
            }
        }

        @Override
        public <T> T getValue(String key, Class<T> type) throws ClassCastException {
            ConfigValue cv = values.get(key);
            if (cv != null) {
                T v = Conversions.convert(cv.unwrap(), type);
                if (v != null) {
                    return v;
                } else {
                    throw new ClassCastException("Can't cast or convert " + cv.unwrap() + " to " + type);
                }
            }
            cv = baseline.getValue(key);
            if (cv != null) {
                T v = Conversions.convert(cv.unwrap(), type);
                if (v != null) {
                    return v;
                } else {
                    throw new ClassCastException("Can't cast or convert " + cv.unwrap() + " to " + type);
                }
            }
            return null;
        }

        @Override
        public <T extends Object> T getValueOrDefault(String key, T defaultValue) {
            Class<T> type = (Class<T>) defaultValue.getClass();
            ConfigValue cv = values.get(key);
            if (cv == null) {
                cv = baseline.getValue(key);
            }
            if (cv != null) {
                T v = Conversions.convert(cv.unwrap(), type);
                if (v != null) {
                    return v;
                } else {
                    return defaultValue;
                }
            } else {
                return defaultValue;
            }
        }

        @Override
        public Builder modify(UUID author) {
            return new Config.Builder(this, author);
        }

        @Override
        public <T> List<T> getValues(String key) {
            ConfigValue cv = values.get(key);
            if (cv != null) {
                Object o = cv.unwrap();
                if (o instanceof Collection) {
                    Collection c = (Collection) o;
                    List<T> ts = new LinkedList<T>();
                    for (Object obj : c) {
                        T t = (T) obj;
                        ts.add(t);
                    }
                    return ts;
                } else {
                    throw new ConfigValueException("Value " + o + " @ " + key + " is not a Collection!");
                }
            }
            List<? extends ConfigValue> cvs = baseline.getValues(key);
            if (cvs == null) {
                return null;
            }
            if (cvs.isEmpty()) {
                List l = cvs;
                return (List<T>) l; // empty list can simply be cast^^
            } else {
                List<T> ts = new LinkedList<T>();
                for (ConfigValue c : cvs) {
                    T t = (T) c.unwrap();
                    ts.add(t);
                }
                return ts;
            }
        }

        @Override
        public <T> List<T> getValues(String key, Class<T> type) {
            ConfigValue cv = values.get(key);
            if (cv != null) {
                Object o = cv.unwrap();
                if (o instanceof Collection) {
                    Collection c = (Collection) o;
                    List<T> ts = new LinkedList<T>();
                    for (Object obj : c) {
                        T t = Conversions.convert(obj, type);
                        if (t != null) {
                            ts.add(t);
                        } else {
                            throw new ClassCastException("Can't cast or convert " + obj + " to " + type);
                        }
                    }
                    return ts;
                } else {
                    throw new ConfigValueException("Value " + o + " @ " + key + " is not a Collection!");
                }
            }
            List<? extends ConfigValue> cvs = baseline.getValues(key);
            if (cvs == null) {
                return null;
            }
            if (cvs.isEmpty()) {
                List l = cvs;
                return (List<T>) l; // empty list can simply be cast^^
            } else {
                List<T> ts = new LinkedList<T>();
                for (ConfigValue c : cvs) {
                    T t = Conversions.convert(c.unwrap(), type);
                    if (t != null) {
                        ts.add(t);
                    } else {
                        throw new ClassCastException("Can't cast or convert " + c.unwrap() + " to " + type);
                    }
                }
                return ts;
            }
        }

        @Override
        public Config copy(boolean newVersionLine) {
            Impl copy;
            if (newVersionLine) {
                copy = new Impl(baseline, version, new AtomicLong(versionFactory.get()));
            } else {
                copy = new Impl(baseline, version, versionFactory);
            }
            for (Entry<String, ConfigValue> e : values.entrySet()) {
                if (e.getValue() instanceof Builder.CV) {
                    Builder.CV cv = (Builder.CV) e.getValue();
                    switch (cv.options.copy) {
                        case SHALLOW:
                            copy.values.put(e.getKey(), cv);
                            break;
                        case DEEP: {
                            Object ocopy = cv.options.cloner.clone(cv.unwrap());
                            Builder.CV newCV = new Builder.CV(ocopy, cv.version, cv.options);
                            copy.values.put(e.getKey(), newCV);
                        }
                        break;
                    }
                } else {
                    copy.values.put(e.getKey(), e.getValue()); // always shallow if not Builder.CV
                }
            }
            return copy;
        }

        public void apply(ConfigUpdate update, Optional<ValueMerger> customMergeLogic) {
            this.version = update.versionId;
            for (Entry<String, ConfigValue> e : update.updates.entrySet()) {
                ConfigValue oldCV = values.get(e.getKey());
                if (oldCV == null) {
                    oldCV = baseline.getValue(e.getKey());
                }
                if (oldCV == null) { // if there's no previous value just insert the new one
                    values.put(e.getKey(), e.getValue());
                } else {
                    if (customMergeLogic.isPresent()) {
                        ConfigValue cv = customMergeLogic.get().merge(e.getKey(), oldCV, e.getValue(), Builder.CVFactory.INSTANCE);
                        values.put(e.getKey(), cv);
                    } else {
                        ConfigValue cv = e.getValue().options().merger.merge(e.getKey(), oldCV, e.getValue(), Builder.CVFactory.INSTANCE);
                        values.put(e.getKey(), cv);
                    }
                }
            }
        }

    }

    public static class Factory {

        public static Config load(BaselineConfig baseline) {
            return new Config.Impl(baseline, 0);
        }
    }

    public static class Builder implements Config {

        private final Config.Impl conf;
        private final HashMap<String, ConfigValue> updates = new HashMap<>();
        private final long versionId;
        private final UUID creator;
        private boolean finalised = false;

        private Builder(Config.Impl conf, UUID creator) {
            this.conf = conf;
            this.versionId = conf.versionFactory.incrementAndGet();
            this.creator = creator;
        }

        @Override
        public <T> Optional<T> readValue(String key) {
            ConfigValue cv = updates.get(key);
            if (cv != null) {
                T v = (T) cv.unwrap();
                return Optional.of(v);
            } else {
                return conf.readValue(key);
            }
        }

        @Override
        public <T> Optional<T> readValue(String key, Class<T> type) {
            try {
                ConfigValue cv = updates.get(key);
                if (cv != null) {
                    T v = Conversions.convert(cv.unwrap(), type);
                    if (v != null) {
                        return Optional.of(v);
                    } else {
                        return Optional.absent();
                    }
                } else {
                    return conf.readValue(key, type);
                }
            } catch (ClassCastException ex) {
                return Optional.absent();
            }
        }

        @Override
        public <T> T getValue(String key, Class<T> type) throws ClassCastException {
            ConfigValue cv = updates.get(key);
            if (cv != null) {
                T v = Conversions.convert(cv.unwrap(), type);
                if (v != null) {
                    return v;
                } else {
                    throw new ClassCastException("Can't cast or convert " + cv.unwrap() + " to " + type);
                }
            } else {
                return conf.getValue(key, type);
            }
        }

        @Override
        public <T> T getValueOrDefault(String key, T defaultValue) {
            Class<T> type = (Class<T>) defaultValue.getClass();
            ConfigValue cv = updates.get(key);
            if ((cv != null) && type.isAssignableFrom(cv.unwrap().getClass())) {
                T v = Conversions.convert(cv.unwrap(), type);
                if (v != null) {
                    return v;
                } else {
                    return defaultValue;
                }
            } else {
                return conf.getValueOrDefault(key, defaultValue);
            }
        }

        @Override
        public Builder modify(UUID author) {
            return this;
        }

        @Override
        public <T> List<T> getValues(String key) {
            ConfigValue cv = updates.get(key);
            if (cv != null) {
                Object o = cv.unwrap();
                if (o instanceof Collection) {
                    Collection c = (Collection) o;
                    List<T> ts = new LinkedList<T>();
                    for (Object obj : c) {
                        T t = (T) obj;
                        ts.add(t);
                    }
                    return ts;
                } else {
                    throw new ConfigValueException("Value " + o + " @ " + key + " is not a Collection!");
                }
            }
            return conf.getValues(key);
        }

        @Override
        public <T> List<T> getValues(String key, Class<T> type) {
            ConfigValue cv = updates.get(key);
            if (cv != null) {
                Object o = cv.unwrap();
                if (o instanceof Collection) {
                    Collection c = (Collection) o;
                    List<T> ts = new LinkedList<T>();
                    for (Object obj : c) {
                        T t = Conversions.convert(obj, type);
                        if (t != null) {
                            ts.add(t);
                        } else {
                            throw new ClassCastException("Can't cast or convert " + obj + " to " + type);
                        }
                    }
                    return ts;
                } else {
                    throw new ConfigValueException("Value " + o + " @ " + key + " is not a Collection!");
                }
            }
            return conf.getValues(key, type);
        }

        // Adders
        /**
         * Sets the value at {@code key} to {@code o}.
         * <p>
         * Uses {@link ValueOptions.DEFAULT}.
         * <p>
         * @param key The location of the target
         * @param o The new value of the target
         */
        public void setValue(String key, Object o) {
            if (finalised) {
                throw new ConfigException("Config Builder has been finalised. No further writes allowed!");
            }
            CV cv = new CV(o, versionId, ValueOptions.DEFAULT);
            this.updates.put(key, cv);
        }

        /**
         * Sets the value at {@code key} to {@code o}.
         * <p>
         * @param key The location of the target
         * @param o The new value of the target
         * @param opts The options associated with the new value
         */
        public void setValue(String key, Object o, ValueOptions opts) {
            if (finalised) {
                throw new ConfigException("Config Builder has been finalised. No further writes allowed!");
            }
            CV cv = new CV(o, versionId, opts);
            this.updates.put(key, cv);
        }

        /**
         * Adds {@code o} to the collection at {@code key}.
         * <p>
         * Starts a new collection if the value doesn't exists or the value
         * isn't currently a collection.
         * <p>
         * @param key The location of the target collection
         * @param o The value to add to the target collection
         */
        public void addValue(String key, Object o) {
            if (finalised) {
                throw new ConfigException("Config Builder has been finalised. No further writes allowed!");
            }
            ConfigValue cv = updates.get(key);
            if (cv != null) {
                Object co = cv.unwrap();
                if (co instanceof Collection) {
                    Collection c = (Collection) co;
                    c.add(o);
                } else {
                    ArrayList l = new ArrayList();
                    l.add(co);
                    l.add(o);
                    CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                    updates.put(key, newCv);
                }
            } else {
                try {
                    List l = conf.getValues(key);
                    if (l != null) {
                        l.add(o);
                        CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                        updates.put(key, newCv);
                    } else {
                        l = new ArrayList();
                        l.add(o);
                        CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                        updates.put(key, newCv);
                    }
                } catch (ConfigValueException ex) {
                    ArrayList l = new ArrayList();
                    l.add(conf.readValue(key).get());
                    l.add(o);
                    CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                    updates.put(key, newCv);
                }
            }
        }

        /**
         * Adds all values in {@code o} to the collection at {@code key}.
         * <p>
         * Starts a new collection if the value doesn't exists or the value
         * isn't currently a collection.
         * <p>
         * @param key The location of the target collection
         * @param os The collection of values to add to the target collection
         */
        public void addValues(String key, Collection<Object> os) {
            if (finalised) {
                throw new ConfigException("Config Builder has been finalised. No further writes allowed!");
            }
            ConfigValue cv = updates.get(key);
            if (cv != null) {
                Object co = cv.unwrap();
                if (co instanceof Collection) {
                    Collection c = (Collection) co;
                    c.addAll(os);
                } else {
                    ArrayList l = new ArrayList();
                    l.add(co);
                    l.addAll(os);
                    CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                    updates.put(key, newCv);
                }
            } else {
                try {
                    List l = conf.getValues(key);
                    if (l != null) {
                        l.addAll(os);
                        CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                        updates.put(key, newCv);
                    } else {
                        l = new ArrayList();
                        l.addAll(os);
                        CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                        updates.put(key, newCv);
                    }
                } catch (ConfigValueException ex) {
                    ArrayList l = new ArrayList();
                    l.add(conf.readValue(key).get());
                    l.addAll(os);
                    CV newCv = new CV(l, versionId, ValueOptions.DEFAULT);
                    updates.put(key, newCv);
                }
            }
        }

        public ConfigUpdate finalise() {
            finalised = true;
            return new ConfigUpdate(updates, versionId, creator);
        }

        @Override
        public Config copy(boolean newVersionLine) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        private static class CV implements ConfigValue {

            private final Object value;
            private final long version;
            private final ValueOptions options;

            private CV(Object o, long versionId, ValueOptions options) {
                this.value = o;
                this.version = versionId;
                this.options = options;
            }

            @Override
            public Object unwrap() {
                return this.value;
            }

            @Override
            public Class<?> type() {
                return this.value.getClass();
            }

            @Override
            public long version() {
                return this.version;
            }

            @Override
            public ValueOptions options() {
                return options;
            }

        }

        static class CVFactory implements ConfigValueFactory {

            public static final ConfigValueFactory INSTANCE = new CVFactory();

            @Override
            public ConfigValue create(Object o, long versionId, ValueOptions opts) {
                return new CV(o, versionId, opts);
            }

        }
    }
}
