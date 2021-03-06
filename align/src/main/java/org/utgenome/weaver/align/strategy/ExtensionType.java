/*--------------------------------------------------------------------------
 *  Copyright 2011 utgenome.org
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *--------------------------------------------------------------------------*/
//--------------------------------------
// genome-weaver Project
//
// ExtensionType.java
// Since: 2011/08/08
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align.strategy;

/**
 * Alignment extension type
 * 
 * @author leo
 * 
 */
public enum ExtensionType {
    MATCH(0), INSERTION(1), DELETION(2);

    private final static ExtensionType[] table = new ExtensionType[] { MATCH, INSERTION, DELETION, MATCH };

    public final int                     code;

    private ExtensionType(int code) {
        this.code = code;
    }

    public static ExtensionType decode(int i) {
        return table[i & 0x3];
    }
}
