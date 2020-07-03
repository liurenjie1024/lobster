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

package com.sun.tools.hat2.internal.server;

import java.io.PrintWriter;

import com.sun.tools.hat2.internal.model.Snapshot;
import com.sun.tools.hat2.internal.util.Misc;
import java.io.StringWriter;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

/**
 *
 * @author      Bill Foote
 */


abstract class QueryHandler {

    protected String urlStart;
    protected String query;
    protected PrintWriter out;
    protected com.sun.tools.hat2.internal.model.Snapshot snapshot;

    abstract void run();


    void setUrlStart(String s) {
        urlStart = s;
    }

    void setQuery(String s) {
        query = s;
    }

    void setOutput(PrintWriter o) {
        this.out = o;
    }

    void setSnapshot(Snapshot ss) {
        this.snapshot = ss;
    }

    protected String encodeForURL(String s) {
        try {
            s = URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            // Should never happen
            ex.printStackTrace();
        }
        return s;
    }

    protected void startHtml(String title) {
        out.print("<html><title>");
        print(title);
        out.println("</title>");
        out.println("<body bgcolor=\"#ffffff\"><center><h1>");
        print(title);
        out.println("</h1></center>");
    }

    protected void endHtml() {
        out.println("</body></html>");
    }

    protected void error(String msg) {
        println(msg);
    }

    protected void printAnchorStart() {
        out.print("<a href=\"");
        out.print(urlStart);
    }

    protected void printThingAnchorTag(long id) {
        printAnchorStart();
        out.print("object/");
        printHex(id);
        out.print("\">");
    }

    protected void printObject(com.sun.tools.hat2.internal.model.JavaObject obj) {
        printThing(obj);
    }

    protected void printThing(com.sun.tools.hat2.internal.model.JavaThing thing) {
        if (thing == null) {
            out.print("null");
            return;
        }
        if (thing instanceof com.sun.tools.hat2.internal.model.JavaHeapObject) {
            com.sun.tools.hat2.internal.model.JavaHeapObject ho = (com.sun.tools.hat2.internal.model.JavaHeapObject) thing;
            long id = ho.getId();
            if (id != -1L) {
                if (ho.isNew())
                out.println("<strong>");
                printThingAnchorTag(id);
            }
            print(thing.toString());
            if (id != -1) {
                if (ho.isNew())
                    out.println("[new]</strong>");
                out.print(" (" + ho.getSize() + " bytes)");
                out.println("</a>");
            }
        } else {
            print(thing.toString());
        }
    }

    protected void printRoot(com.sun.tools.hat2.internal.model.Root root) {
        com.sun.tools.hat2.internal.model.StackTrace st = root.getStackTrace();
        boolean traceAvailable = (st != null) && (st.getFrames().length != 0);
        if (traceAvailable) {
            printAnchorStart();
            out.print("rootStack/");
            printHex(root.getIndex());
            out.print("\">");
        }
        print(root.getDescription());
        if (traceAvailable) {
            out.print("</a>");
        }
    }

    protected void printClass(com.sun.tools.hat2.internal.model.JavaClass clazz) {
        if (clazz == null) {
            out.println("null");
            return;
        }
        printAnchorStart();
        out.print("class/");
        print(encodeForURL(clazz));
        out.print("\">");
        print(clazz.toString());
        out.println("</a>");
    }

    protected String encodeForURL(com.sun.tools.hat2.internal.model.JavaClass clazz) {
        if (clazz.getId() == -1) {
            return encodeForURL(clazz.getName());
        } else {
            return clazz.getIdString();
        }
    }

    protected void printField(com.sun.tools.hat2.internal.model.JavaField field) {
        print(field.getName() + " (" + field.getSignature() + ")");
    }

    protected void printStatic(com.sun.tools.hat2.internal.model.JavaStatic member) {
        com.sun.tools.hat2.internal.model.JavaField f = member.getField();
        printField(f);
        out.print(" : ");
        if (f.hasId()) {
            com.sun.tools.hat2.internal.model.JavaThing t = member.getValue();
            printThing(t);
        } else {
            print(member.getValue().toString());
        }
    }

    protected void printStackTrace(com.sun.tools.hat2.internal.model.StackTrace trace) {
        com.sun.tools.hat2.internal.model.StackFrame[] frames = trace.getFrames();
        for (int i = 0; i < frames.length; i++) {
            com.sun.tools.hat2.internal.model.StackFrame f = frames[i];
            String clazz = f.getClassName();
            out.print("<font color=purple>");
            print(clazz);
            out.print("</font>");
            print("." + f.getMethodName() + "(" + f.getMethodSignature() + ")");
            out.print(" <bold>:</bold> ");
            print(f.getSourceFileName() + " line " + f.getLineNumber());
            out.println("<br>");
        }
    }

    protected void printException(Throwable t) {
        println(t.getMessage());
        out.println("<pre>");
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        print(sw.toString());
        out.println("</pre>");
    }

    protected void printHex(long addr) {
        if (snapshot.getIdentifierSize() == 4) {
            out.print(Misc.toHex((int)addr));
        } else {
            out.print(Misc.toHex(addr));
        }
    }

    protected long parseHex(String value) {
        return Misc.parseHex(value);
    }

    protected void print(String str) {
        out.print(Misc.encodeHtml(str));
    }

    protected void println(String str) {
        out.println(Misc.encodeHtml(str));
    }
}
