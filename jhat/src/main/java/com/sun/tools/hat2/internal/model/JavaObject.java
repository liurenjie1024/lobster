/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package com.sun.tools.hat2.internal.model;

import java.io.IOException;

import com.sun.tools.hat2.internal.parser.ReadBuffer;

/**
 * Represents Java instance
 *
 * @author      Bill Foote
 */
public class JavaObject extends JavaLazyReadObject {

    private Object clazz;       // Number before resolve
                                // JavaClass after resolve
    /**
     * Construct a new JavaObject.
     *
     * @param classID id of the class object
     * @param offset The offset of field data
     */
    public JavaObject(long classID, long offset) {
        super(offset);
        this.clazz = makeId(classID);
    }

    public void resolve(com.sun.tools.hat2.internal.model.Snapshot snapshot) {
        if (clazz instanceof com.sun.tools.hat2.internal.model.JavaClass) {
            return;
        }
        if (clazz instanceof Number) {
            long classID = getIdValue((Number)clazz);
            clazz = snapshot.findThing(classID);
            if (! (clazz instanceof com.sun.tools.hat2.internal.model.JavaClass)) {
                warn("Class " + Long.toHexString(classID) + " not found, " +
                     "adding fake class!");
                int length;
                ReadBuffer buf = snapshot.getReadBuffer();
                int idSize = snapshot.getIdentifierSize();
                long lenOffset = getOffset() + 2*idSize + 4;
                try {
                    length = buf.getInt(lenOffset);
                } catch (IOException exp) {
                    throw new RuntimeException(exp);
                }
                clazz = snapshot.addFakeInstanceClass(classID, length);
            }
        } else {
            throw new InternalError("should not reach here");
        }

        com.sun.tools.hat2.internal.model.JavaClass cl = (com.sun.tools.hat2.internal.model.JavaClass) clazz;
        cl.resolve(snapshot);

        // while resolving, parse fields in verbose mode.
        // but, getFields calls parseFields in non-verbose mode
        // to avoid printing warnings repeatedly.
        parseFields(getValue(), true);

        cl.addInstance(this);
        super.resolve(snapshot);
    }

    /**
     * Are we the same type as other?  We are iff our clazz is the
     * same type as other's.
     */
    public boolean isSameTypeAs(com.sun.tools.hat2.internal.model.JavaThing other) {
        if (!(other instanceof JavaObject)) {
            return false;
        }
        JavaObject oo = (JavaObject) other;
        return getClazz().equals(oo.getClazz());
    }

    /**
     * Return our JavaClass object.  This may only be called after resolve.
     */
    public com.sun.tools.hat2.internal.model.JavaClass getClazz() {
        return (com.sun.tools.hat2.internal.model.JavaClass) clazz;
    }

    public com.sun.tools.hat2.internal.model.JavaThing[] getFields() {
        // pass false to verbose mode so that dereference
        // warnings are not printed.
        return parseFields(getValue(), false);
    }

    // returns the value of field of given name
    public com.sun.tools.hat2.internal.model.JavaThing getField(String name) {
        com.sun.tools.hat2.internal.model.JavaThing[] flds = getFields();
        com.sun.tools.hat2.internal.model.JavaField[] instFields = getClazz().getFieldsForInstance();
        for (int i = 0; i < instFields.length; i++) {
            if (instFields[i].getName().equals(name)) {
                return flds[i];
            }
        }
        return null;
    }

    public int compareTo(com.sun.tools.hat2.internal.model.JavaThing other) {
        if (other instanceof JavaObject) {
            JavaObject oo = (JavaObject) other;
            return getClazz().getName().compareTo(oo.getClazz().getName());
        }
        return super.compareTo(other);
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
        super.visitReferencedObjects(v);
        com.sun.tools.hat2.internal.model.JavaThing[] flds = getFields();
        for (int i = 0; i < flds.length; i++) {
            if (flds[i] != null) {
                if (v.mightExclude()
                    && v.exclude(getClazz().getClassForField(i),
                                 getClazz().getFieldForInstance(i)))
                {
                    // skip it
                } else if (flds[i] instanceof com.sun.tools.hat2.internal.model.JavaHeapObject) {
                    v.visit((JavaHeapObject) flds[i]);
                }
            }
        }
    }

    public boolean refersOnlyWeaklyTo(com.sun.tools.hat2.internal.model.Snapshot ss, com.sun.tools.hat2.internal.model.JavaThing other) {
        if (ss.getWeakReferenceClass() != null) {
            final int referentFieldIndex = ss.getReferentFieldIndex();
            if (ss.getWeakReferenceClass().isAssignableFrom(getClazz())) {
                //
                // REMIND:  This introduces a dependency on the JDK
                //      implementation that is undesirable.
                com.sun.tools.hat2.internal.model.JavaThing[] flds = getFields();
                for (int i = 0; i < flds.length; i++) {
                    if (i != referentFieldIndex && flds[i] == other) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
    public String describeReferenceTo(com.sun.tools.hat2.internal.model.JavaThing target, com.sun.tools.hat2.internal.model.Snapshot ss) {
        com.sun.tools.hat2.internal.model.JavaThing[] flds = getFields();
        for (int i = 0; i < flds.length; i++) {
            if (flds[i] == target) {
                com.sun.tools.hat2.internal.model.JavaField f = getClazz().getFieldForInstance(i);
                return "field " + f.getName();
            }
        }
        return super.describeReferenceTo(target, ss);
    }

    public String toString() {
        if (getClazz().isString()) {
            com.sun.tools.hat2.internal.model.JavaThing value = getField("value");
            if (value instanceof com.sun.tools.hat2.internal.model.JavaValueArray) {
                return ((JavaValueArray)value).valueString();
            } else {
                return "null";
            }
        } else {
            return super.toString();
        }
    }

    // Internals only below this point

    /*
     * Java instance record (HPROF_GC_INSTANCE_DUMP) looks as below:
     *
     *     object ID
     *     stack trace serial number (int)
     *     class ID
     *     data length (int)
     *     byte[length]
     */
    protected final int readValueLength() throws IOException {
        com.sun.tools.hat2.internal.model.JavaClass cl = getClazz();
        int idSize = cl.getIdentifierSize();
        long lengthOffset = getOffset() + 2*idSize + 4;
        return cl.getReadBuffer().getInt(lengthOffset);
    }

    protected final byte[] readValue() throws IOException {
        com.sun.tools.hat2.internal.model.JavaClass cl = getClazz();
        int idSize = cl.getIdentifierSize();
        ReadBuffer buf = cl.getReadBuffer();
        long offset = getOffset() + 2*idSize + 4;
        int length = buf.getInt(offset);
        if (length == 0) {
            return com.sun.tools.hat2.internal.model.Snapshot.EMPTY_BYTE_ARRAY;
        } else {
            byte[] res = new byte[length];
            buf.get(offset + 4, res);
            return res;
        }
    }

    private com.sun.tools.hat2.internal.model.JavaThing[] parseFields(byte[] data, boolean verbose) {
        com.sun.tools.hat2.internal.model.JavaClass cl = getClazz();
        int target = cl.getNumFieldsForInstance();
        com.sun.tools.hat2.internal.model.JavaField[] fields = cl.getFields();
        com.sun.tools.hat2.internal.model.JavaThing[] fieldValues = new JavaThing[target];
        Snapshot snapshot = cl.getSnapshot();
        int idSize = snapshot.getIdentifierSize();
        int fieldNo = 0;
        // In the dump file, the fields are stored in this order:
        // fields of most derived class (immediate class) are stored
        // first and then the super class and so on. In this object,
        // fields are stored in the reverse ("natural") order. i.e.,
        // fields of most super class are stored first.

        // target variable is used to compensate for the fact that
        // the dump file starts field values from the leaf working
        // upwards in the inheritance hierarchy, whereas JavaObject
        // starts with the top of the inheritance hierarchy and works down.
        target -= fields.length;
        JavaClass currClass = cl;
        int index = 0;
        for (int i = 0; i < fieldValues.length; i++, fieldNo++) {
            while (fieldNo >= fields.length) {
                currClass = currClass.getSuperclass();
                fields = currClass.getFields();
                fieldNo = 0;
                target -= fields.length;
            }
            JavaField f = fields[fieldNo];
            char sig = f.getSignature().charAt(0);
            switch (sig) {
                case 'L':
                case '[': {
                    long id = objectIdAt(index, data);
                    index += idSize;
                    com.sun.tools.hat2.internal.model.JavaObjectRef ref = new JavaObjectRef(id);
                    fieldValues[target+fieldNo] = ref.dereference(snapshot, f, verbose);
                    break;
                }
                case 'Z': {
                    byte value = byteAt(index, data);
                    index++;
                    fieldValues[target+fieldNo] = new JavaBoolean(value != 0);
                    break;
                }
                case 'B': {
                    byte value = byteAt(index, data);
                    index++;
                    fieldValues[target+fieldNo] = new JavaByte(value);
                    break;
                }
                case 'S': {
                    short value = shortAt(index, data);
                    index += 2;
                    fieldValues[target+fieldNo] = new JavaShort(value);
                    break;
                }
                case 'C': {
                    char value = charAt(index, data);
                    index += 2;
                    fieldValues[target+fieldNo] = new JavaChar(value);
                    break;
                }
                case 'I': {
                    int value = intAt(index, data);
                    index += 4;
                    fieldValues[target+fieldNo] = new JavaInt(value);
                    break;
                }
                case 'J': {
                    long value = longAt(index, data);
                    index += 8;
                    fieldValues[target+fieldNo] = new JavaLong(value);
                    break;
                }
                case 'F': {
                    float value = floatAt(index, data);
                    index += 4;
                    fieldValues[target+fieldNo] = new JavaFloat(value);
                    break;
                }
                case 'D': {
                    double value = doubleAt(index, data);
                    index += 8;
                    fieldValues[target+fieldNo] = new JavaDouble(value);
                    break;
                }
                default:
                    throw new RuntimeException("invalid signature: " + sig);
            }
        }
        return fieldValues;
    }

    private void warn(String msg) {
        System.out.println("WARNING: " + msg);
    }
}
