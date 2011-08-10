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
// SuffixFilter.java
// Since: 2011/07/27
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align.strategy;

import org.utgenome.weaver.align.ACGT;
import org.utgenome.weaver.align.ACGTSequence;
import org.utgenome.weaver.align.BitVector;
import org.utgenome.weaver.align.SuffixInterval;
import org.xerial.util.log.Logger;

/**
 * Suffix filter
 * 
 * <pre>
 *   *---*---*---*
 *   | \ | \ | \ |
 *   *---*---*---*
 *   | \ | \ | \ |
 *   *---*---*---*
 * 
 * </pre>
 * 
 * @author leo
 * 
 */
public class SuffixFilter
{
    private static Logger   _logger = Logger.getLogger(SuffixFilter.class);

    private final int       k;
    private final int       m;

    // automaton

    private StaircaseFilter staircaseFilter;
    private QueryMask       queryMask;

    public static class Candidate
    {
        public final SuffixInterval si;
        public final int            k;
        public final int            offset;

        public Candidate(SuffixInterval si, int k, int offset) {
            this.si = si;
            this.k = k;
            this.offset = offset;
        }
    }

    public enum SearchFlag {
        A(1), C(1 << 1), G(1 << 2), T(1 << 3), Split(1 << 4);
        private int flag;

        private SearchFlag(int flag) {
            this.flag = flag;
        }
    }

    private enum State {
        Finished, NoMatch, Match
    }

    private static class DFSState
    {
        public final State type;
        public final int   matchRow;

        public DFSState(State state, int matchRow) {
            this.type = state;
            this.matchRow = matchRow;
        }

        @Override
        public String toString() {
            return String.format("type:%s, match row:%d", type, matchRow);
        }
    }

    public class SearchState
    {
        public final BitVector[] automaton;

        // 32 bit = searchFlag (8) + minK (8) + index (16)  
        public int               state = 0;

        private SearchState(BitVector[] automaton, int minK, int index) {
            this.automaton = automaton;
            this.state = ((minK & 0xFF) << 8) | ((index & 0xFFFF) << 16);
        }

        public SearchState initialState(int k, int m) {
            BitVector[] automaton = new BitVector[k + 1];
            // Activate the diagonal states 
            for (int i = 0; i <= k; ++i) {
                automaton[i] = new BitVector(m);
                // TODO optimize
                for (int j = 0; j <= i; ++j)
                    automaton[i].set(j);
            }
            SearchState s = new SearchState(automaton, 0, 0);
            return s;
        }

        public SearchState nextState(ACGT ch) {
            this.state |= 1 << ch.code;

            final int k = automaton.length - 1;
            final int m = (int) automaton[0].size();

            BitVector[] prev = automaton;
            BitVector[] next = new BitVector[k + 1];

            final BitVector qeq = queryMask.getPatternMask(ch);

            final int nextIndex = (state >>> 16) + 1;

            int nm = 0;
            // R'_0 = (R_0 << 1) | P[ch]
            next[nm] = prev[nm].rshift(1)._and(qeq);
            if (next[nm].get(m - 1)) {
                // Found a full match
                //return new DFSState(State.Finished, 0);
                return new SearchState(next, nm, nextIndex);
            }
            if (!next[nm].get(nextIndex))
                ++nm;

            for (int i = 1; i <= k; ++i) {
                // R'_{i+1} = ((R_{i+1} << 1) &  P[ch]) | R_i | (R_i << 1) | (R'_i << 1)   
                next[i] = prev[i].rshift(1)._and(qeq);
                next[i]._or(prev[i - 1]);
                next[i]._or(prev[i - 1].rshift(1));
                next[i]._or(next[i - 1].rshift(1));
                // Apply a suffix filter (staircase mask)
                next[i]._and(staircaseFilter.getStaircaseMask(i));

                // Found a match
                if (next[i].get(m - 1))
                    return new SearchState(next, i, nextIndex);

                if (nm == i && !next[i].get(nextIndex))
                    ++nm;
            }

            if (nm >= k) {
                // no match
                return null;
            }
            else {
                // extend the match
                return new SearchState(next, nm, nextIndex);
            }
        }
    }

    /**
     * A set of bit flags of ACGT characters in a query sequence
     * 
     * @author leo
     * 
     */
    public static class QueryMask
    {
        private BitVector[] patternMask;

        public QueryMask(ACGTSequence query) {
            this(query, 0);
        }

        public QueryMask(ACGTSequence query, int offset) {
            int m = (int) query.textSize();
            patternMask = new BitVector[ACGT.exceptN.length];
            for (int i = 0; i < patternMask.length; ++i)
                patternMask[i] = new BitVector(m);

            for (int i = 0; i < m; ++i) {
                int index = offset + i;
                if (index >= m) {
                    // for bidirectional search
                    index = m - i - 1;
                }
                ACGT ch = query.getACGT(index);
                if (ch == ACGT.N) {
                    for (ACGT each : ACGT.exceptN)
                        patternMask[ch.code].set(i);
                }
                else
                    patternMask[ch.code].set(i);
            }
        }

        public BitVector getPatternMask(ACGT ch) {
            return patternMask[ch.code];
        }
    }

    public SuffixFilter(int k, ACGTSequence query) {
        this.k = k;
        this.m = (int) query.textSize();
        this.staircaseFilter = new StaircaseFilter(m, k);
        this.queryMask = new QueryMask(query);
    }

}
