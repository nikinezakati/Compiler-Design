/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.xerces.dom;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;

/**
 * The DOMImplementation class is description of a particular
 * implementation of the Document Object Model. As such its data is
 * static, shared by all instances of this implementation.
 * <P>
 * The DOM API requires that it be a real object rather than static
 * methods. However, there's nothing that says it can't be a singleton,
 * so that's how I've implemented it.
 * 
 * @xerces.internal
 *
 * @version $Id$
 * @since  PR-DOM-Level-1-19980818.
 */
public class DOMImplementationImpl extends CoreDOMImplementationImpl
    implements DOMImplementation {

    //
    // Data
    //

    // static

    /** Dom implementation singleton. */
    static final DOMImplementationImpl singleton = new DOMImplementationImpl();


    //
    // Public methods
    //

    /** NON-DOM: Obtain and return the single shared object */
    public static DOMImplementation getDOMImplementation() {
        return singleton;
    }

    //
    // DOMImplementation methods
    //

    /**
     * Test if the DOM implementation supports a specific "feature" --
     * currently meaning language and level thereof.
     *
     * @param feature      The package name of the feature to test.
     * In Level 1, supported values are "HTML" and "XML" (case-insensitive).
     * At this writing, org.apache.xerces.dom supports only XML.
     *
     * @param version      The version number of the feature being tested.
     * This is interpreted as "Version of the DOM API supported for the
     * specified Feature", and in Level 1 should be "1.0"
     *
     * @return    true iff this implementation is compatable with the
     * specified feature and version.
     */
    public boolean hasFeature(String feature, String version) {

        boolean result = super.hasFeature(feature, version);
        if (!result) {
            boolean anyVersion = version == null || version.length() == 0;
            if (feature.startsWith("+")) {
                feature = feature.substring(1);
            }
            return (
                (feature.equalsIgnoreCase("Events")
                    && (anyVersion || version.equals("2.0")))
                    || (feature.equalsIgnoreCase("MutationEvents")
                        && (anyVersion || version.equals("2.0")))
                    || (feature.equalsIgnoreCase("Traversal")
                        && (anyVersion || version.equals("2.0")))
                    || (feature.equalsIgnoreCase("Range")
                        && (anyVersion || version.equals("2.0")))
                    || (feature.equalsIgnoreCase("MutationEvents")
                        && (anyVersion || version.equals("2.0"))));
        }
        return result;
    } // hasFeature(String,String):boolean
    
    //
    // Protected methods
    //
    
    protected CoreDocumentImpl createDocument(DocumentType doctype) {
        return new DocumentImpl(doctype);
    }

} // class DOMImplementationImpl
