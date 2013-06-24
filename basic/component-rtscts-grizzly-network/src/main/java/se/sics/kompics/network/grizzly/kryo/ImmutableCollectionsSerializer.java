/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.grizzly.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.RegisteredClass;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.serialize.IntSerializer;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
public class ImmutableCollectionsSerializer extends Serializer {

    private final Kryo _kryo;

    /**
     * @param kryo the kryo instance
     */
    public ImmutableCollectionsSerializer(final Kryo kryo) {
        _kryo = kryo;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked")
    @Override
    public <T> T readObjectData(final ByteBuffer buffer, final Class<T> clazz) {
        final int ordinal = IntSerializer.get(buffer, true);
        final ImmutableCollectionEnum unmodifiableCollection = ImmutableCollectionEnum.values()[ordinal];
        final RegisteredClass sourceClass = _kryo.readClass(buffer);
        System.out.println("Registered: " + sourceClass.getType());
        final Object sourceCollection = _kryo.readClassAndObject(buffer);
        return (T) unmodifiableCollection.create(sourceCollection);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeObjectData(final ByteBuffer buffer, final Object object) {
        try {
            final ImmutableCollectionEnum unmodifiableCollection = ImmutableCollectionEnum.valueOfType(object.getClass());
            // the ordinal could be replaced by s.th. else (e.g. a explicitely managed "id")
            IntSerializer.put(buffer, unmodifiableCollection.ordinal(), true);
            _kryo.writeClassAndObject(buffer, unmodifiableCollection.sourceCollectionType);
        } catch (final RuntimeException e) {
            // Don't eat and wrap RuntimeExceptions because the ObjectBuffer.write...
            // handles SerializationException specifically (resizing the buffer)...
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static enum ImmutableCollectionEnum {
        
//        COLLECTION(ImmutableCollection.class, Collection.class) {
//            @Override
//            public Object create(final Object sourceCollection) {
//                return ImmutableList.copyOf((List<?>) sourceCollection);
//            }
//        },
        LIST(ImmutableList.class, ArrayList.class) {
            @Override
            public Object create(final Object sourceCollection) {
                return ImmutableList.copyOf((List<?>) sourceCollection);
            }
        },
        SET(ImmutableSet.class, HashSet.class) {
            @Override
            public Object create(final Object sourceCollection) {
                return ImmutableSet.copyOf((Set<?>) sourceCollection);
            }
        },
        SORTED_SET(ImmutableSortedSet.class, TreeSet.class) {
            @Override
            public Object create(final Object sourceCollection) {
                return ImmutableSortedSet.copyOf((SortedSet<?>) sourceCollection);
            }
        },
        MAP(ImmutableMap.class, HashMap.class) {
            @Override
            public Object create(final Object sourceCollection) {
                return ImmutableMap.copyOf((Map<?, ?>) sourceCollection);
            }
        },
        SORTED_MAP(ImmutableSortedMap.class, TreeMap.class) {
            @Override
            public Object create(final Object sourceCollection) {
                return ImmutableSortedMap.copyOf((SortedMap<?, ?>) sourceCollection);
            }
        };
        private final Class<?> type;
        private final Class<?> sourceCollectionType;

        private ImmutableCollectionEnum(final Class<?> type, Class<?> sourceCollectionType) {
            this.type = type;
            this.sourceCollectionType = sourceCollectionType;
        }

        /**
         * @param sourceCollection
         */
        public abstract Object create(Object sourceCollection);

        static ImmutableCollectionEnum valueOfType(final Class<?> type) {
            for (final ImmutableCollectionEnum item : values()) {
                if (item.type.isAssignableFrom(type)) {
                    return item;
                }
            }
            throw new IllegalArgumentException("The type " + type + " is not supported.");
        }
    }

    /**
     * Creates a new {@link UnmodifiableCollectionsSerializer} and registers its
     * serializer for the several unmodifiable Collections that can be created
     * via {@link Collections}, including {@link Map}s.
     *
     * @param kryo the {@link Kryo} instance to set the serializer on.
     *
     * @see Collections#unmodifiableCollection(Collection)
     * @see Collections#unmodifiableList(List)
     * @see Collections#unmodifiableSet(Set)
     * @see Collections#unmodifiableSortedSet(SortedSet)
     * @see Collections#unmodifiableMap(Map)
     * @see Collections#unmodifiableSortedMap(SortedMap)
     */
    public static void registerSerializers(final Kryo kryo) {
        final ImmutableCollectionsSerializer serializer = new ImmutableCollectionsSerializer(kryo);
        ImmutableCollectionEnum.values();
        for (final ImmutableCollectionEnum item : ImmutableCollectionEnum.values()) {
            kryo.register(item.type, serializer);
        }
    }
}
