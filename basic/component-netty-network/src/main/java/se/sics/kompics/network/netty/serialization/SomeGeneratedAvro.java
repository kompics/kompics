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
package se.sics.kompics.network.netty.serialization;  
@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class SomeGeneratedAvro extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SomeGeneratedAvro\",\"namespace\":\"se.sics.kompics.network.netty.serialization\",\"fields\":[{\"name\":\"someNumber\",\"type\":\"int\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public int someNumber;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>. 
   */
  public SomeGeneratedAvro() {}

  /**
   * All-args constructor.
   */
  public SomeGeneratedAvro(java.lang.Integer someNumber) {
    this.someNumber = someNumber;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call. 
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return someNumber;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }
  // Used by DatumReader.  Applications should not call. 
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: someNumber = (java.lang.Integer)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'someNumber' field.
   */
  public java.lang.Integer getSomeNumber() {
    return someNumber;
  }

  /**
   * Sets the value of the 'someNumber' field.
   * @param value the value to set.
   */
  public void setSomeNumber(java.lang.Integer value) {
    this.someNumber = value;
  }

  /** Creates a new SomeGeneratedAvro RecordBuilder */
  public static se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder newBuilder() {
    return new se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder();
  }
  
  /** Creates a new SomeGeneratedAvro RecordBuilder by copying an existing Builder */
  public static se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder newBuilder(se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder other) {
    return new se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder(other);
  }
  
  /** Creates a new SomeGeneratedAvro RecordBuilder by copying an existing SomeGeneratedAvro instance */
  public static se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder newBuilder(se.sics.kompics.network.netty.serialization.SomeGeneratedAvro other) {
    return new se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder(other);
  }
  
  /**
   * RecordBuilder for SomeGeneratedAvro instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<SomeGeneratedAvro>
    implements org.apache.avro.data.RecordBuilder<SomeGeneratedAvro> {

    private int someNumber;

    /** Creates a new Builder */
    private Builder() {
      super(se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.SCHEMA$);
    }
    
    /** Creates a Builder by copying an existing Builder */
    private Builder(se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.someNumber)) {
        this.someNumber = data().deepCopy(fields()[0].schema(), other.someNumber);
        fieldSetFlags()[0] = true;
      }
    }
    
    /** Creates a Builder by copying an existing SomeGeneratedAvro instance */
    private Builder(se.sics.kompics.network.netty.serialization.SomeGeneratedAvro other) {
            super(se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.SCHEMA$);
      if (isValidValue(fields()[0], other.someNumber)) {
        this.someNumber = data().deepCopy(fields()[0].schema(), other.someNumber);
        fieldSetFlags()[0] = true;
      }
    }

    /** Gets the value of the 'someNumber' field */
    public java.lang.Integer getSomeNumber() {
      return someNumber;
    }
    
    /** Sets the value of the 'someNumber' field */
    public se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder setSomeNumber(int value) {
      validate(fields()[0], value);
      this.someNumber = value;
      fieldSetFlags()[0] = true;
      return this; 
    }
    
    /** Checks whether the 'someNumber' field has been set */
    public boolean hasSomeNumber() {
      return fieldSetFlags()[0];
    }
    
    /** Clears the value of the 'someNumber' field */
    public se.sics.kompics.network.netty.serialization.SomeGeneratedAvro.Builder clearSomeNumber() {
      fieldSetFlags()[0] = false;
      return this;
    }

    @Override
    public SomeGeneratedAvro build() {
      try {
        SomeGeneratedAvro record = new SomeGeneratedAvro();
        record.someNumber = fieldSetFlags()[0] ? this.someNumber : (java.lang.Integer) defaultValue(fields()[0]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }
}
