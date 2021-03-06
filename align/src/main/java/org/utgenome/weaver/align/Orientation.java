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
// Strand.java
// Since: 2011/02/16
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

public enum Orientation {
    FORWARD("F"), REVERSE("R");

    public final String symbol;

    private Orientation(String symbol) {
        this.symbol = symbol;
    }

    public boolean isForward() {
        return this == FORWARD;
    }

    @Override
    public String toString() {
        return symbol;
    }

    public Orientation opposite() {
        if (this == FORWARD)
            return REVERSE;
        else
            return FORWARD;
    }

}
