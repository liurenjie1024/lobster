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

import java.util.Vector;
import java.util.Enumeration;

import com.sun.tools.hat2.internal.util.CompositeEnumeration;
import com.sun.tools.hat2.internal.parser.ReadBuffer;

/**
 *
 * @author      Bill Foote
 */


public class JavaClass extends com.sun.tools.hat2.internal.model.JavaHeapObject {
    // my id
    private long id;
    // my name
    private String name;

    // These are JavaObjectRef before resolve
    private com.sun.tools.hat2.internal.model.JavaThing superclass;
    private com.sun.tools.hat2.internal.model.JavaThing loader;
    private com.sun.tools.hat2.internal.model.JavaThing signers;
    private com.sun.tools.hat2.internal.model.JavaThing protectionDomain;

    // non-static fields
    private com.sun.tools.hat2.internal.model.JavaField[] fields;
    // static fields
    private com.sun.tools.hat2.internal.model.JavaStatic[] statics;

    private static final JavaClass[] EMPTY_CLASS_ARRAY = new JavaClass[0];
    // my subclasses
    private JavaClass[] subclasses = EMPTY_CLASS_ARRAY;

    // my instances
    private Vector<com.sun.tools.hat2.internal.model.JavaHeapObject> instances = new Vector<com.sun.tools.hat2.internal.model.JavaHeapObject>();

    // Who I belong to.  Set on resolve.
    private com.sun.tools.hat2.internal.model.Snapshot mySnapshot;

    // Size of an instance, including VM overhead
    private int instanceSize;
    // Total number of fields including inherited ones
    private int totalNumFields;


    public JavaClass(long id, String name, long superclassId, long loaderId,
                     long signersId, long protDomainId,
                     com.sun.tools.hat2.internal.model.JavaField[] fields, com.sun.tools.hat2.internal.model.JavaStatic[] statics,
                     int instanceSize) {
        this.id = id;
        this.name = name;
        this.superclass = new com.sun.tools.hat2.internal.model.JavaObjectRef(superclassId);
        this.loader = new com.sun.tools.hat2.internal.model.JavaObjectRef(loaderId);
        this.signers = new com.sun.tools.hat2.internal.model.JavaObjectRef(signersId);
        this.protectionDomain = new JavaObjectRef(protDomainId);
        this.fields = fields;
        this.statics = statics;
        this.instanceSize = instanceSize;
    }

    public JavaClass(String name, long superclassId, long loaderId,
                     long signersId, long protDomainId,
                     com.sun.tools.hat2.internal.model.JavaField[] fields, com.sun.tools.hat2.internal.model.JavaStatic[] statics,
                     int instanceSize) {
        this(-1L, name, superclassId, loaderId, signersId,
             protDomainId, fields, statics, instanceSize);
    }

    public final JavaClass getClazz() {
        return mySnapshot.getJavaLangClass();
    }

    public final int getIdentifierSize() {
        return mySnapshot.getIdentifierSize();
    }

    public final int getMinimumObjectSize() {
        return mySnapshot.getMinimumObjectSize();
    }

    public void resolve(com.sun.tools.hat2.internal.model.Snapshot snapshot) {
        if (mySnapshot != null) {
            return;
        }
        mySnapshot = snapshot;
        resolveSuperclass(snapshot);
        if (superclass != null) {
            ((JavaClass) superclass).addSubclass(this);
        }

        loader  = loader.dereference(snapshot, null);
        signers  = signers.dereference(snapshot, null);
        protectionDomain  = protectionDomain.dereference(snapshot, null);

        for (int i = 0; i < statics.length; i++) {
            statics[i].resolve(this, snapshot);
        }
        snapshot.getJavaLangClass().addInstance(this);
        super.resolve(snapshot);
        return;
    }

    /**
     * Resolve our superclass.  This might be called well before
     * all instances are available (like when reading deferred
     * instances in a 1.2 dump file :-)  Calling this is sufficient
     * to be able to explore this class' fields.
     */
    public void resolveSuperclass(com.sun.tools.hat2.internal.model.Snapshot snapshot) {
        if (superclass == null) {
            // We must be java.lang.Object, so we have no superclass.
        } else {
            totalNumFields = fields.length;
            superclass = superclass.dereference(snapshot, null);
            if (superclass == snapshot.getNullThing()) {
                superclass = null;
            } else {
                try {
                    JavaClass sc = (JavaClass) superclass;
                    sc.resolveSuperclass(snapshot);
                    totalNumFields += sc.totalNumFields;
                } catch (ClassCastException ex) {
                    System.out.println("Warning!  Superclass of " + name + " is " + superclass);
                    superclass = null;
                }
            }
        }
    }

    public boolean isString() {
        return mySnapshot.getJavaLangString() == this;
    }

    public boolean isClassLoader() {
        return mySnapshot.getJavaLangClassLoader().isAssignableFrom(this);
    }

    /**
     * Get a numbered field from this class
     */
    public com.sun.tools.hat2.internal.model.JavaField getField(int i) {
        if (i < 0 || i >= fields.length) {
            throw new Error("No field " + i + " for " + name);
        }
        return fields[i];
    }

    /**
     * Get the total number of fields that are part of an instance of
     * this class.  That is, include superclasses.
     */
    public int getNumFieldsForInstance() {
        return totalNumFields;
    }

    /**
     * Get a numbered field from all the fields that are part of instance
     * of this class.  That is, include superclasses.
     */
    public com.sun.tools.hat2.internal.model.JavaField getFieldForInstance(int i) {
        if (superclass != null) {
            JavaClass sc = (JavaClass) superclass;
            if (i < sc.totalNumFields) {
                return sc.getFieldForInstance(i);
            }
            i -= sc.totalNumFields;
        }
        return getField(i);
    }

    /**
     * Get the class responsible for field i, where i is a field number that
     * could be passed into getFieldForInstance.
     *
     * @see JavaClass.getFieldForInstance()
     */
    public JavaClass getClassForField(int i) {
        if (superclass != null) {
            JavaClass sc = (JavaClass) superclass;
            if (i < sc.totalNumFields) {
                return sc.getClassForField(i);
            }
        }
        return this;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isArray() {
        return name.indexOf('[') != -1;
    }

    public Enumeration getInstances(boolean includeSubclasses) {
        if (includeSubclasses) {
            Enumeration res = instances.elements();
            for (int i = 0; i < subclasses.length; i++) {
                res = new CompositeEnumeration(res,
                              subclasses[i].getInstances(true));
            }
            return res;
        } else {
            return instances.elements();
        }
    }

    /**
     * @return a count of the instances of this class
     */
    public int getInstancesCount(boolean includeSubclasses) {
        int result = instances.size();
        if (includeSubclasses) {
            for (int i = 0; i < subclasses.length; i++) {
                result += subclasses[i].getInstancesCount(includeSubclasses);
            }
        }
        return result;
    }

    public JavaClass[] getSubclasses() {
        return subclasses;
    }

    /**
     * This can only safely be called after resolve()
     */
    public JavaClass getSuperclass() {
        return (JavaClass) superclass;
    }

    /**
     * This can only safely be called after resolve()
     */
    public com.sun.tools.hat2.internal.model.JavaThing getLoader() {
        return loader;
    }

    /**
     * This can only safely be called after resolve()
     */
    public boolean isBootstrap() {
        return loader == mySnapshot.getNullThing();
    }

    /**
     * This can only safely be called after resolve()
     */
    public com.sun.tools.hat2.internal.model.JavaThing getSigners() {
        return signers;
    }

    /**
     * This can only safely be called after resolve()
     */
    public com.sun.tools.hat2.internal.model.JavaThing getProtectionDomain() {
        return protectionDomain;
    }

    public com.sun.tools.hat2.internal.model.JavaField[] getFields() {
        return fields;
    }

    /**
     * Includes superclass fields
     */
    public com.sun.tools.hat2.internal.model.JavaField[] getFieldsForInstance() {
        Vector<com.sun.tools.hat2.internal.model.JavaField> v = new Vector<com.sun.tools.hat2.internal.model.JavaField>();
        addFields(v);
        com.sun.tools.hat2.internal.model.JavaField[] result = new com.sun.tools.hat2.internal.model.JavaField[v.size()];
        for (int i = 0; i < v.size(); i++) {
            result[i] =  v.elementAt(i);
        }
        return result;
    }


    public com.sun.tools.hat2.internal.model.JavaStatic[] getStatics() {
        return statics;
    }

    // returns value of static field of given name
    public com.sun.tools.hat2.internal.model.JavaThing getStaticField(String name) {
        for (int i = 0; i < statics.length; i++) {
            JavaStatic s = statics[i];
            if (s.getField().getName().equals(name)) {
                return s.getValue();
            }
        }
        return null;
    }

    public String toString() {
        return "class " + name;
    }

    public int compareTo(com.sun.tools.hat2.internal.model.JavaThing other) {
        if (other instanceof JavaClass) {
            return name.compareTo(((JavaClass) other).name);
        }
        return super.compareTo(other);
    }


    /**
     * @return true iff a variable of type this is assignable from an instance
     *          of other
     */
    public boolean isAssignableFrom(JavaClass other) {
        if (this == other) {
            return true;
        } else if (other == null) {
            return false;
        } else {
            return isAssignableFrom((JavaClass) other.superclass);
            // Trivial tail recursion:  I have faith in javac.
        }
    }

    /**
     * Describe the reference that this thing has to target.  This will only
     * be called if target is in the array returned by getChildrenForRootset.
     */
     public String describeReferenceTo(com.sun.tools.hat2.internal.model.JavaThing target, com.sun.tools.hat2.internal.model.Snapshot ss) {
        for (int i = 0; i < statics.length; i++) {
            com.sun.tools.hat2.internal.model.JavaField f = statics[i].getField();
            if (f.hasId()) {
                com.sun.tools.hat2.internal.model.JavaThing other = statics[i].getValue();
                if (other == target) {
                    return "static field " + f.getName();
                }
            }
        }
        return super.describeReferenceTo(target, ss);
    }

    /**
     * @return the size of an instance of this class.  Gives 0 for an array
     *          type.
     */
    public int getInstanceSize() {
        return instanceSize + mySnapshot.getMinimumObjectSize();
    }


    /**
     * @return The size of all instances of this class.  Correctly handles
     *          arrays.
     */
    public long getTotalInstanceSize() {
        int count = instances.size();
        if (count == 0 || !isArray()) {
            return count * instanceSize;
        }

        // array class and non-zero count, we have to
        // get the size of each instance and sum it
        long result = 0;
        for (int i = 0; i < count; i++) {
            com.sun.tools.hat2.internal.model.JavaThing t = (com.sun.tools.hat2.internal.model.JavaThing) instances.elementAt(i);
            result += t.getSize();
        }
        return result;
    }

    /**
     * @return the size of this object
     */
    public int getSize() {
        JavaClass cl = mySnapshot.getJavaLangClass();
        if (cl == null) {
            return 0;
        } else {
            return cl.getInstanceSize();
        }
    }

    public void visitReferencedObjects(JavaHeapObjectVisitor v) {
        super.visitReferencedObjects(v);
        com.sun.tools.hat2.internal.model.JavaHeapObject sc = getSuperclass();
        if (sc != null) v.visit(getSuperclass());

        JavaThing other;
        other = getLoader();
        if (other instanceof com.sun.tools.hat2.internal.model.JavaHeapObject) {
            v.visit((com.sun.tools.hat2.internal.model.JavaHeapObject)other);
        }
        other = getSigners();
        if (other instanceof com.sun.tools.hat2.internal.model.JavaHeapObject) {
            v.visit((com.sun.tools.hat2.internal.model.JavaHeapObject)other);
        }
        other = getProtectionDomain();
        if (other instanceof com.sun.tools.hat2.internal.model.JavaHeapObject) {
            v.visit((com.sun.tools.hat2.internal.model.JavaHeapObject)other);
        }

        for (int i = 0; i < statics.length; i++) {
            com.sun.tools.hat2.internal.model.JavaField f = statics[i].getField();
            if (!v.exclude(this, f) && f.hasId()) {
                other = statics[i].getValue();
                if (other instanceof com.sun.tools.hat2.internal.model.JavaHeapObject) {
                    v.visit((com.sun.tools.hat2.internal.model.JavaHeapObject) other);
                }
            }
        }
    }

    // package-privates below this point
    final ReadBuffer getReadBuffer() {
        return mySnapshot.getReadBuffer();
    }

    final void setNew(com.sun.tools.hat2.internal.model.JavaHeapObject obj, boolean flag) {
        mySnapshot.setNew(obj, flag);
    }

    final boolean isNew(com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        return mySnapshot.isNew(obj);
    }

    final StackTrace getSiteTrace(com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        return mySnapshot.getSiteTrace(obj);
    }

    final void addReferenceFromRoot(com.sun.tools.hat2.internal.model.Root root, com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        mySnapshot.addReferenceFromRoot(root, obj);
    }

    final Root getRoot(com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        return mySnapshot.getRoot(obj);
    }

    final Snapshot getSnapshot() {
        return mySnapshot;
    }

    void addInstance(com.sun.tools.hat2.internal.model.JavaHeapObject inst) {
        instances.addElement(inst);
    }

    // Internals only below this point
    private void addFields(Vector<JavaField> v) {
        if (superclass != null) {
            ((JavaClass) superclass).addFields(v);
        }
        for (int i = 0; i < fields.length; i++) {
            v.addElement(fields[i]);
        }
    }

    private void addSubclassInstances(Vector<JavaHeapObject> v) {
        for (int i = 0; i < subclasses.length; i++) {
            subclasses[i].addSubclassInstances(v);
        }
        for (int i = 0; i < instances.size(); i++) {
            v.addElement(instances.elementAt(i));
        }
    }

    private void addSubclass(JavaClass sub) {
        JavaClass newValue[] = new JavaClass[subclasses.length + 1];
        System.arraycopy(subclasses, 0, newValue, 0, subclasses.length);
        newValue[subclasses.length] = sub;
        subclasses = newValue;
    }
}
