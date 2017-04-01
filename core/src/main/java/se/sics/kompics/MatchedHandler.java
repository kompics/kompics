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
package se.sics.kompics;

/**
 *
 * @author lkroll
 * @param <P> The type of the pattern to match against
 * @param <V> The type of the content value
 * @param <E> The type of context event
 */
public abstract class MatchedHandler<P, V, E extends KompicsEvent & PatternExtractor<P, ? super V>> {

  Class<E> cxtType = null;

  protected MatchedHandler() {
  }

  protected MatchedHandler(Class<E> cxtType) {
    cxtType = cxtType;
  }

  public abstract P pattern();

  public abstract void handle(V content, E context);

  /**
   * Sets the event type.
   * <p>
   * @param eventType
   * the new event type
   */
  public void setCxtType(Class<E> cxtType) {
    this.cxtType = cxtType;
  }

  /**
   * Gets the event type.
   * <p>
   * @return the event type
   */
  public Class<E> getCxtType() {
    return cxtType;
  }
}
