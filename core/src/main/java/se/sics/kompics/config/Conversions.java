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

import com.google.common.collect.HashMultimap;

/**
 *
 * @author lkroll
 */
public abstract class Conversions {

    private static final HashMultimap<Class<?>, Converter<?>> converters = HashMultimap.create();

    static {
        // Numbers
        Converter<Long> longConv = new Converter<Long>() {

            @Override
            public Long convert(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return n.longValue();
                }
                if (o instanceof String) {
                    return Long.parseLong((String) o);
                }
                return null;
            }

            @Override
            public Class<Long> type() {
                return Long.class;
            }
        };
        converters.put(longConv.type(), longConv);
        Converter<Integer> intConv = new Converter<Integer>() {

            @Override
            public Integer convert(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return n.intValue();
                }
                if (o instanceof String) {
                    return Integer.parseInt((String) o);
                }
                return null;
            }

            @Override
            public Class<Integer> type() {
                return Integer.class;
            }
        };
        converters.put(intConv.type(), intConv);
        Converter<Byte> byteConv = new Converter<Byte>() {

            @Override
            public Byte convert(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return n.byteValue();
                }
                if (o instanceof String) {
                    return Byte.parseByte((String) o);
                }
                return null;
            }

            @Override
            public Class<Byte> type() {
                return Byte.class;
            }
        };
        converters.put(byteConv.type(), byteConv);
        Converter<Short> shortConv = new Converter<Short>() {

            @Override
            public Short convert(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return n.shortValue();
                }
                if (o instanceof String) {
                    return Short.parseShort((String) o);
                }
                return null;
            }

            @Override
            public Class<Short> type() {
                return Short.class;
            }
        };
        converters.put(shortConv.type(), shortConv);
        Converter<Float> floatConv = new Converter<Float>() {

            @Override
            public Float convert(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return n.floatValue();
                }
                if (o instanceof String) {
                    return Float.parseFloat((String) o);
                }
                return null;
            }

            @Override
            public Class<Float> type() {
                return Float.class;
            }
        };
        converters.put(floatConv.type(), floatConv);
        Converter<Double> doubleConv = new Converter<Double>() {

            @Override
            public Double convert(Object o) {
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return n.doubleValue();
                }
                if (o instanceof String) {
                    return Double.parseDouble((String) o);
                }
                return null;
            }

            @Override
            public Class<Double> type() {
                return Double.class;
            }
        };
        converters.put(doubleConv.type(), doubleConv);

        // String
        Converter<String> strConv = new Converter<String>() {

            @Override
            public String convert(Object o) {
                return o.toString();
            }

            @Override
            public Class<String> type() {
                return String.class;
            }
        };
        converters.put(strConv.type(), strConv);

        // Boolean
        Converter<Boolean> boolConv = new Converter<Boolean>() {

            @Override
            public Boolean convert(Object o) {
                if (o instanceof String) {
                    String s = (String) o;
                    switch (s.toLowerCase()) {
                        case "true":
                            return true;
                        case "yes":
                            return true;
                        case "t":
                            return true;
                        case "y":
                            return true;
                        case "1":
                            return true;
                        case "false":
                            return false;
                        case "no":
                            return false;
                        case "f":
                            return false;
                        case "n":
                            return false;
                        case "0":
                            return false;
                        default:
                            return null;
                    }
                }
                if (o instanceof Number) {
                    Number n = (Number) o;
                    return ((n.longValue() == 0) || (n.doubleValue() == 0.0));
                }
                return null;
            }

            @Override
            public Class<Boolean> type() {
                return Boolean.class;
            }
        };
        converters.put(boolConv.type(), boolConv);
    }

    public static <T> T convert(Object o, Class<T> type) {
//        System.out.println("Converters: \n" + converters.toString());
//        System.out.println("Trying to convert " + o + "(" + o.getClass() + ") to " + type);
        if (type.isInstance(o)) {
            return type.cast(o);
        }
        T val = null;
        for (Converter c : converters.get(type)) {
            //System.out.println("Trying converter " + c + " with type " + c.type());
            Converter<T> tc = c; // must be possible because it was at type in converters
            try {
                val = tc.convert(o);
            } catch (Exception ex) {
                // do nothing, simply assume this converter didn't work
            }
        }
        //System.out.println("Final value is " + val);
        return val; // last write wins
    }

    public static void register(Converter c) {
        converters.put(c.type(), c);
    }
}
