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
package se.sics.kompics.util;

import java.util.Optional;
import se.sics.kompics.PatternExtractor;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class PatternExtractorHelper {

  public static Object peelAllLayers(PatternExtractor obj) {
    PatternExtractor o = obj;
    while (o.extractValue() instanceof PatternExtractor) {
      o = (PatternExtractor) o.extractValue();
    }
    return o.extractValue();
  }
  
  public static Object peelOneLayer(PatternExtractor obj) {
    return obj.extractValue();
  }

  public static Optional<PatternExtractor> peelToLayer(PatternExtractor obj, Object patternType) {
    PatternExtractor o = obj;
    while(true) {
      if(o.extractPattern().equals(patternType)) {
        return Optional.of(o);
      }
      Object value = o.extractValue();
      if(!(value instanceof PatternExtractor)) {
        return Optional.empty();
      }
      o = (PatternExtractor)value;
    }
  }
}
