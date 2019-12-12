/*
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
package se.sics.kompics;

/**
 * Base class for matching handlers, which use an extracted value to "pattern match" against.
 * 
 * A pattern matching handler separates an incoming event into a <i>context</i> and a <i>content</i> part. It passes
 * them separately to the {@link #handle(Object, PatternExtractor)} method.
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 * @param <P>
 *            The type of the pattern to match against
 * @param <V>
 *            The type of the content value
 * @param <E>
 *            The type of context event
 */
public abstract class MatchedHandler<P, V, E extends PatternExtractor<P, ? super V>> {

    Class<E> contextType = null;

    protected MatchedHandler() {
    }

    protected MatchedHandler(Class<E> contextType) {
        this.contextType = contextType;
    }

    public abstract P pattern();

    public abstract void handle(V content, E context);

    /**
     * Sets the context type.
     * <p>
     * 
     * @param cxtType
     *            the context type
     * @deprecated Since 1.2.0, use {@link #setContextType(Class)} instead. Method will be dropped in 1.3.0!
     */
    @Deprecated
    public void setCxtType(Class<E> cxtType) {
        this.contextType = cxtType;
    }

    /**
     * Sets the context type.
     * 
     * @param contextType
     *            the type of the context
     */
    public void setContextType(Class<E> contextType) {
        this.contextType = contextType;
    }

    /**
     * Gets the context type.
     * <p>
     * 
     * @return the context type
     * @deprecated Since 1.2.0, use {@link #getContextType()} instead. Method will be dropped in 1.3.0!
     */
    @Deprecated
    public Class<E> getCxtType() {
        return contextType;
    }

    /**
     * Gets the context type.
     * 
     * @return the context type
     */
    public Class<E> getContextType() {
        return contextType;
    }
}
