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
// FMIndexOnOccTable.java
// Since: 2011/05/16
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import org.utgenome.gwt.utgb.client.bio.IUPAC;

public class FMIndexOnOccTable implements FMIndex
{
    private final IUPACSequence        seq;
    private final OccurrenceCountTable occ;
    private final CharacterCount       C;

    public FMIndexOnOccTable(IUPACSequence seq, int windowSize) {
        this.seq = seq;
        this.occ = new OccurrenceCountTable(seq, windowSize);
        this.C = new CharacterCount(seq);
    }

    public CharacterCount getCharacterCount() {
        return C;
    }

    public SuffixInterval backwardSearch(IUPAC ch, SuffixInterval current) {
        long lowerBound = C.getCharacterCountSmallerThan(ch) + occ.getOcc(ch, current.lowerBound);
        long upperBound = C.getCharacterCountSmallerThan(ch) + occ.getOcc(ch, current.upperBound + 1) - 1;
        return new SuffixInterval(lowerBound, upperBound);
    }

    /**
     * Follow the suffix link using the equation: SA[x] - 1 = C(x) + Rank(c, x).
     * 
     * @param index
     *            index x in the suffix array
     * @return index p in the suffix array that satisfies SA[p] = SA[x] - 1.
     */
    public long suffixLink(long index) {
        if (index >= seq.textSize()) { // If the index reaches the sentinel 
            return 0; // Return the smallest SA index
        }
        IUPAC c = IUPAC.decode((byte) seq.lookup(index));
        return C.getCharacterCountSmallerThan(c) + occ.getOcc(c, index);
    }

    public long textSize() {
        return seq.textSize();
    }

}