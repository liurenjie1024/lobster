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

import java.lang.ref.SoftReference;
import java.util.*;

import com.sun.tools.hat2.internal.parser.ReadBuffer;
import com.sun.tools.hat2.internal.util.Misc;

/**
 *
 * @author      Bill Foote
 */

/**
 * Represents a snapshot of the Java objects in the VM at one instant.
 * This is the top-level "model" object read out of a single .hprof or .bod
 * file.
 */

public class Snapshot {

    public static long SMALL_ID_MASK = 0x0FFFFFFFFL;
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

    private static final com.sun.tools.hat2.internal.model.JavaField[] EMPTY_FIELD_ARRAY = new com.sun.tools.hat2.internal.model.JavaField[0];
    private static final com.sun.tools.hat2.internal.model.JavaStatic[] EMPTY_STATIC_ARRAY = new JavaStatic[0];

    // all heap objects
    private Hashtable<Number, com.sun.tools.hat2.internal.model.JavaHeapObject> heapObjects =
                 new Hashtable<Number, com.sun.tools.hat2.internal.model.JavaHeapObject>();

    private Hashtable<Number, com.sun.tools.hat2.internal.model.JavaClass> fakeClasses =
                 new Hashtable<Number, com.sun.tools.hat2.internal.model.JavaClass>();

    // all Roots in this Snapshot
    private Vector<com.sun.tools.hat2.internal.model.Root> roots = new Vector<com.sun.tools.hat2.internal.model.Root>();

    // name-to-class map
    private Map<String, com.sun.tools.hat2.internal.model.JavaClass> classes =
                 new TreeMap<String, com.sun.tools.hat2.internal.model.JavaClass>();

    // new objects relative to a baseline - lazily initialized
    private volatile Map<com.sun.tools.hat2.internal.model.JavaHeapObject, Boolean> newObjects;

    // allocation site traces for all objects - lazily initialized
    private volatile Map<com.sun.tools.hat2.internal.model.JavaHeapObject, com.sun.tools.hat2.internal.model.StackTrace> siteTraces;

    // object-to-Root map for all objects
    private Map<com.sun.tools.hat2.internal.model.JavaHeapObject, com.sun.tools.hat2.internal.model.Root> rootsMap =
                 new HashMap<com.sun.tools.hat2.internal.model.JavaHeapObject, com.sun.tools.hat2.internal.model.Root>();

    // soft cache of finalizeable objects - lazily initialized
    private SoftReference<Vector> finalizablesCache;

    // represents null reference
    private com.sun.tools.hat2.internal.model.JavaThing nullThing;

    // java.lang.ref.Reference class
    private com.sun.tools.hat2.internal.model.JavaClass weakReferenceClass;
    // index of 'referent' field in java.lang.ref.Reference class
    private int referentFieldIndex;

    // java.lang.Class class
    private com.sun.tools.hat2.internal.model.JavaClass javaLangClass;
    // java.lang.String class
    private com.sun.tools.hat2.internal.model.JavaClass javaLangString;
    // java.lang.ClassLoader class
    private com.sun.tools.hat2.internal.model.JavaClass javaLangClassLoader;

    // unknown "other" array class
    private volatile com.sun.tools.hat2.internal.model.JavaClass otherArrayType;
    // Stuff to exclude from reachable query
    private com.sun.tools.hat2.internal.model.ReachableExcludes reachableExcludes;
    // the underlying heap dump buffer
    private ReadBuffer readBuf;

    // True iff some heap objects have isNew set
    private boolean hasNewSet;
    private boolean unresolvedObjectsOK;

    // whether object array instances have new style class or
    // old style (element) class.
    private boolean newStyleArrayClass;

    // object id size in the heap dump
    private int identifierSize = 4;

    // minimum object size - accounts for object header in
    // most Java virtual machines - we assume 2 identifierSize
    // (which is true for Sun's hotspot JVM).
    private int minimumObjectSize;

    public Snapshot(ReadBuffer buf) {
        nullThing = new HackJavaValue("<null>", 0);
        readBuf = buf;
    }

    public void setSiteTrace(com.sun.tools.hat2.internal.model.JavaHeapObject obj, com.sun.tools.hat2.internal.model.StackTrace trace) {
        if (trace != null && trace.getFrames().length != 0) {
            initSiteTraces();
            siteTraces.put(obj, trace);
        }
    }

    public com.sun.tools.hat2.internal.model.StackTrace getSiteTrace(
        com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        if (siteTraces != null) {
            return siteTraces.get(obj);
        } else {
            return null;
        }
    }

    public void setNewStyleArrayClass(boolean value) {
        newStyleArrayClass = value;
    }

    public boolean isNewStyleArrayClass() {
        return newStyleArrayClass;
    }

    public void setIdentifierSize(int size) {
        identifierSize = size;
        minimumObjectSize = 2 * size;
    }

    public int getIdentifierSize() {
        return identifierSize;
    }

    public int getMinimumObjectSize() {
        return minimumObjectSize;
    }

    public void addHeapObject(long id, com.sun.tools.hat2.internal.model.JavaHeapObject ho) {
        heapObjects.put(makeId(id), ho);
    }

    public void addRoot(com.sun.tools.hat2.internal.model.Root r) {
        r.setIndex(roots.size());
        roots.addElement(r);
    }

    public void addClass(long id, com.sun.tools.hat2.internal.model.JavaClass c) {
        addHeapObject(id, c);
        putInClassesMap(c);
    }

    com.sun.tools.hat2.internal.model.JavaClass addFakeInstanceClass(long classID, int instSize) {
        // Create a fake class name based on ID.
        String name = "unknown-class<@" + Misc.toHex(classID) + ">";

        // Create fake fields convering the given instance size.
        // Create as many as int type fields and for the left over
        // size create byte type fields.
        int numInts = instSize / 4;
        int numBytes = instSize % 4;
        com.sun.tools.hat2.internal.model.JavaField[] fields = new com.sun.tools.hat2.internal.model.JavaField[numInts + numBytes];
        int i;
        for (i = 0; i < numInts; i++) {
            fields[i] = new com.sun.tools.hat2.internal.model.JavaField("unknown-field-" + i, "I");
        }
        for (i = 0; i < numBytes; i++) {
            fields[i + numInts] = new com.sun.tools.hat2.internal.model.JavaField("unknown-field-" +
                                                i + numInts, "B");
        }

        // Create fake instance class
        com.sun.tools.hat2.internal.model.JavaClass
            c = new com.sun.tools.hat2.internal.model.JavaClass(name, 0, 0, 0, 0, fields,
                                 EMPTY_STATIC_ARRAY, instSize);
        // Add the class
        addFakeClass(makeId(classID), c);
        return c;
    }


    /**
     * @return true iff it's possible that some JavaThing instances might
     *          isNew set
     *
     * @see com.sun.tools.hat2.internal.model.JavaThing.isNew()
     */
    public boolean getHasNewSet() {
        return hasNewSet;
    }

    //
    // Used in the body of resolve()
    //
    private static class MyVisitor extends AbstractJavaHeapObjectVisitor {
        com.sun.tools.hat2.internal.model.JavaHeapObject t;
        public void visit(com.sun.tools.hat2.internal.model.JavaHeapObject other) {
            other.addReferenceFrom(t);
        }
    }

    // To show heap parsing progress, we print a '.' after this limit
    private static final int DOT_LIMIT = 5000;

    /**
     * Called after reading complete, to initialize the structure
     */
    public void resolve(boolean calculateRefs) {
        System.out.println("Resolving " + heapObjects.size() + " objects...");

        // First, resolve the classes.  All classes must be resolved before
        // we try any objects, because the objects use classes in their
        // resolution.
        javaLangClass = findClass("java.lang.Class");
        if (javaLangClass == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.Class!");
            javaLangClass = new com.sun.tools.hat2.internal.model.JavaClass("java.lang.Class", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClass);
        }
        javaLangString = findClass("java.lang.String");
        if (javaLangString == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.String!");
            javaLangString = new com.sun.tools.hat2.internal.model.JavaClass("java.lang.String", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangString);
        }
        javaLangClassLoader = findClass("java.lang.ClassLoader");
        if (javaLangClassLoader == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.ClassLoader!");
            javaLangClassLoader = new com.sun.tools.hat2.internal.model.JavaClass("java.lang.ClassLoader", 0, 0, 0, 0,
                                 EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClassLoader);
        }

        for (com.sun.tools.hat2.internal.model.JavaHeapObject t : heapObjects.values()) {
            if (t instanceof com.sun.tools.hat2.internal.model.JavaClass) {
                t.resolve(this);
            }
        }

        // Now, resolve everything else.
        for (com.sun.tools.hat2.internal.model.JavaHeapObject t : heapObjects.values()) {
            if (!(t instanceof com.sun.tools.hat2.internal.model.JavaClass)) {
                t.resolve(this);
            }
        }

        heapObjects.putAll(fakeClasses);
        fakeClasses.clear();

        weakReferenceClass = findClass("java.lang.ref.Reference");
        if (weakReferenceClass == null)  {      // JDK 1.1.x
            weakReferenceClass = findClass("sun.misc.Ref");
            referentFieldIndex = 0;
        } else {
            JavaField[] fields = weakReferenceClass.getFieldsForInstance();
            for (int i = 0; i < fields.length; i++) {
                if ("referent".equals(fields[i].getName())) {
                    referentFieldIndex = i;
                    break;
                }
            }
        }

        if (calculateRefs) {
            calculateReferencesToObjects();
            System.out.print("Eliminating duplicate references");
            System.out.flush();
            // This println refers to the *next* step
        }
        int count = 0;
        for (com.sun.tools.hat2.internal.model.JavaHeapObject t : heapObjects.values()) {
            t.setupReferers();
            ++count;
            if (calculateRefs && count % DOT_LIMIT == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        if (calculateRefs) {
            System.out.println("");
        }

        // to ensure that Iterator.remove() on getClasses()
        // result will throw exception..
        classes = Collections.unmodifiableMap(classes);
    }

    private void calculateReferencesToObjects() {
        System.out.print("Chasing references, expect "
                         + (heapObjects.size() / DOT_LIMIT) + " dots");
        System.out.flush();
        int count = 0;
        MyVisitor visitor = new MyVisitor();
        for (com.sun.tools.hat2.internal.model.JavaHeapObject t : heapObjects.values()) {
            visitor.t = t;
            // call addReferenceFrom(t) on all objects t references:
            t.visitReferencedObjects(visitor);
            ++count;
            if (count % DOT_LIMIT == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        System.out.println();
        for (com.sun.tools.hat2.internal.model.Root r : roots) {
            r.resolve(this);
            com.sun.tools.hat2.internal.model.JavaHeapObject t = findThing(r.getId());
            if (t != null) {
                t.addReferenceFromRoot(r);
            }
        }
    }

    public void markNewRelativeTo(Snapshot baseline) {
        hasNewSet = true;
        for (com.sun.tools.hat2.internal.model.JavaHeapObject t : heapObjects.values()) {
            boolean isNew;
            long thingID = t.getId();
            if (thingID == 0L || thingID == -1L) {
                isNew = false;
            } else {
                com.sun.tools.hat2.internal.model.JavaThing other = baseline.findThing(t.getId());
                if (other == null) {
                    isNew = true;
                } else {
                    isNew = !t.isSameTypeAs(other);
                }
            }
            t.setNew(isNew);
        }
    }

    public Enumeration<com.sun.tools.hat2.internal.model.JavaHeapObject> getThings() {
        return heapObjects.elements();
    }


    public com.sun.tools.hat2.internal.model.JavaHeapObject findThing(long id) {
        Number idObj = makeId(id);
        com.sun.tools.hat2.internal.model.JavaHeapObject jho = heapObjects.get(idObj);
        return jho != null? jho : fakeClasses.get(idObj);
    }

    public com.sun.tools.hat2.internal.model.JavaHeapObject findThing(String id) {
        return findThing(Misc.parseHex(id));
    }

    public com.sun.tools.hat2.internal.model.JavaClass findClass(String name) {
        if (name.startsWith("0x")) {
            return (com.sun.tools.hat2.internal.model.JavaClass) findThing(name);
        } else {
            return classes.get(name);
        }
    }

    /**
     * Return an Iterator of all of the classes in this snapshot.
     **/
    public Iterator getClasses() {
        // note that because classes is a TreeMap
        // classes are already sorted by name
        return classes.values().iterator();
    }

    public com.sun.tools.hat2.internal.model.JavaClass[] getClassesArray() {
        com.sun.tools.hat2.internal.model.JavaClass[] res = new com.sun.tools.hat2.internal.model.JavaClass[classes.size()];
        classes.values().toArray(res);
        return res;
    }

    public synchronized Enumeration getFinalizerObjects() {
        Vector obj;
        if (finalizablesCache != null &&
            (obj = finalizablesCache.get()) != null) {
            return obj.elements();
        }

        com.sun.tools.hat2.internal.model.JavaClass clazz = findClass("java.lang.ref.Finalizer");
        com.sun.tools.hat2.internal.model.JavaObject queue = (com.sun.tools.hat2.internal.model.JavaObject) clazz.getStaticField("queue");
        com.sun.tools.hat2.internal.model.JavaThing tmp = queue.getField("head");
        Vector<com.sun.tools.hat2.internal.model.JavaHeapObject> finalizables = new Vector<com.sun.tools.hat2.internal.model.JavaHeapObject>();
        if (tmp != getNullThing()) {
            com.sun.tools.hat2.internal.model.JavaObject head = (com.sun.tools.hat2.internal.model.JavaObject) tmp;
            while (true) {
                com.sun.tools.hat2.internal.model.JavaHeapObject
                    referent = (com.sun.tools.hat2.internal.model.JavaHeapObject) head.getField("referent");
                com.sun.tools.hat2.internal.model.JavaThing next = head.getField("next");
                if (next == getNullThing() || next.equals(head)) {
                    break;
                }
                head = (JavaObject) next;
                finalizables.add(referent);
            }
        }
        finalizablesCache = new SoftReference<Vector>(finalizables);
        return finalizables.elements();
    }

    public Enumeration<com.sun.tools.hat2.internal.model.Root> getRoots() {
        return roots.elements();
    }

    public com.sun.tools.hat2.internal.model.Root[] getRootsArray() {
        com.sun.tools.hat2.internal.model.Root[] res = new com.sun.tools.hat2.internal.model.Root[roots.size()];
        roots.toArray(res);
        return res;
    }

    public com.sun.tools.hat2.internal.model.Root getRootAt(int i) {
        return roots.elementAt(i);
    }

    public com.sun.tools.hat2.internal.model.ReferenceChain[]
    rootsetReferencesTo(com.sun.tools.hat2.internal.model.JavaHeapObject target, boolean includeWeak) {
        Vector<com.sun.tools.hat2.internal.model.ReferenceChain> fifo = new Vector<com.sun.tools.hat2.internal.model.ReferenceChain>();  // This is slow... A real fifo would help
            // Must be a fifo to go breadth-first
        Hashtable<com.sun.tools.hat2.internal.model.JavaHeapObject, com.sun.tools.hat2.internal.model.JavaHeapObject> visited = new Hashtable<com.sun.tools.hat2.internal.model.JavaHeapObject, com.sun.tools.hat2.internal.model.JavaHeapObject>();
        // Objects are added here right after being added to fifo.
        Vector<com.sun.tools.hat2.internal.model.ReferenceChain> result = new Vector<com.sun.tools.hat2.internal.model.ReferenceChain>();
        visited.put(target, target);
        fifo.addElement(new com.sun.tools.hat2.internal.model.ReferenceChain(target, null));

        while (fifo.size() > 0) {
            com.sun.tools.hat2.internal.model.ReferenceChain chain = fifo.elementAt(0);
            fifo.removeElementAt(0);
            com.sun.tools.hat2.internal.model.JavaHeapObject curr = chain.getObj();
            if (curr.getRoot() != null) {
                result.addElement(chain);
                // Even though curr is in the rootset, we want to explore its
                // referers, because they might be more interesting.
            }
            Enumeration referers = curr.getReferers();
            while (referers.hasMoreElements()) {
                com.sun.tools.hat2.internal.model.JavaHeapObject t = (com.sun.tools.hat2.internal.model.JavaHeapObject) referers.nextElement();
                if (t != null && !visited.containsKey(t)) {
                    if (includeWeak || !t.refersOnlyWeaklyTo(this, curr)) {
                        visited.put(t, t);
                        fifo.addElement(new com.sun.tools.hat2.internal.model.ReferenceChain(t, chain));
                    }
                }
            }
        }

        com.sun.tools.hat2.internal.model.ReferenceChain[] realResult = new ReferenceChain[result.size()];
        for (int i = 0; i < result.size(); i++) {
            realResult[i] =  result.elementAt(i);
        }
        return realResult;
    }

    public boolean getUnresolvedObjectsOK() {
        return unresolvedObjectsOK;
    }

    public void setUnresolvedObjectsOK(boolean v) {
        unresolvedObjectsOK = v;
    }

    public com.sun.tools.hat2.internal.model.JavaClass getWeakReferenceClass() {
        return weakReferenceClass;
    }

    public int getReferentFieldIndex() {
        return referentFieldIndex;
    }

    public JavaThing getNullThing() {
        return nullThing;
    }

    public void setReachableExcludes(com.sun.tools.hat2.internal.model.ReachableExcludes e) {
        reachableExcludes = e;
    }

    public ReachableExcludes getReachableExcludes() {
        return reachableExcludes;
    }

    // package privates
    void addReferenceFromRoot(com.sun.tools.hat2.internal.model.Root r, com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        com.sun.tools.hat2.internal.model.Root root = rootsMap.get(obj);
        if (root == null) {
            rootsMap.put(obj, r);
        } else {
            rootsMap.put(obj, root.mostInteresting(r));
        }
    }

    Root getRoot(com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        return rootsMap.get(obj);
    }

    com.sun.tools.hat2.internal.model.JavaClass getJavaLangClass() {
        return javaLangClass;
    }

    com.sun.tools.hat2.internal.model.JavaClass getJavaLangString() {
        return javaLangString;
    }

    com.sun.tools.hat2.internal.model.JavaClass getJavaLangClassLoader() {
        return javaLangClassLoader;
    }

    com.sun.tools.hat2.internal.model.JavaClass getOtherArrayType() {
        if (otherArrayType == null) {
            synchronized(this) {
                if (otherArrayType == null) {
                    addFakeClass(new com.sun.tools.hat2.internal.model.JavaClass("[<other>", 0, 0, 0, 0,
                                     EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY,
                                     0));
                    otherArrayType = findClass("[<other>");
                }
            }
        }
        return otherArrayType;
    }

    com.sun.tools.hat2.internal.model.JavaClass getArrayClass(String elementSignature) {
        com.sun.tools.hat2.internal.model.JavaClass clazz;
        synchronized(classes) {
            clazz = findClass("[" + elementSignature);
            if (clazz == null) {
                clazz = new com.sun.tools.hat2.internal.model.JavaClass("[" + elementSignature, 0, 0, 0, 0,
                                   EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
                addFakeClass(clazz);
                // This is needed because the JDK only creates Class structures
                // for array element types, not the arrays themselves.  For
                // analysis, though, we need to pretend that there's a
                // JavaClass for the array type, too.
            }
        }
        return clazz;
    }

    ReadBuffer getReadBuffer() {
        return readBuf;
    }

    void setNew(com.sun.tools.hat2.internal.model.JavaHeapObject obj, boolean isNew) {
        initNewObjects();
        if (isNew) {
            newObjects.put(obj, Boolean.TRUE);
        }
    }

    boolean isNew(com.sun.tools.hat2.internal.model.JavaHeapObject obj) {
        if (newObjects != null) {
            return newObjects.get(obj) != null;
        } else {
            return false;
        }
    }

    // Internals only below this point
    private Number makeId(long id) {
        if (identifierSize == 4) {
            return new Integer((int)id);
        } else {
            return new Long(id);
        }
    }

    private void putInClassesMap(com.sun.tools.hat2.internal.model.JavaClass c) {
        String name = c.getName();
        if (classes.containsKey(name)) {
            // more than one class can have the same name
            // if so, create a unique name by appending
            // - and id string to it.
            name += "-" + c.getIdString();
        }
        classes.put(c.getName(), c);
    }

    private void addFakeClass(com.sun.tools.hat2.internal.model.JavaClass c) {
        putInClassesMap(c);
        c.resolve(this);
    }

    private void addFakeClass(Number id, JavaClass c) {
        fakeClasses.put(id, c);
        addFakeClass(c);
    }

    private synchronized void initNewObjects() {
        if (newObjects == null) {
            synchronized (this) {
                if (newObjects == null) {
                    newObjects = new HashMap<com.sun.tools.hat2.internal.model.JavaHeapObject, Boolean>();
                }
            }
        }
    }

    private synchronized void initSiteTraces() {
        if (siteTraces == null) {
            synchronized (this) {
                if (siteTraces == null) {
                    siteTraces = new HashMap<JavaHeapObject, StackTrace>();
                }
            }
        }
    }
}
