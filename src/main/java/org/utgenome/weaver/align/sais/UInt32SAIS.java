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
// Uint32SAIS.java
// Since: 2011/02/17
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align.sais;

import java.util.Arrays;

import org.utgenome.weaver.align.LSeq;
import org.utgenome.weaver.align.RSBitVector;

/**
 * Suffix-array (SA) construction algorithm based on Induced Sorting (IS)
 * 
 * @author leo
 * 
 */
public class UInt32SAIS
{
    private final LSeq           T;
    private final long           N;
    private final int            K;
    private final long[]         bucketStart;
    private RSBitVector          LS;

    private final static boolean LType = false;
    private final static boolean SType = true;

    static class ArrayWrap implements LSeq
    {

        private final LSeq seq;
        private final long offset;
        private final long length;

        public ArrayWrap(LSeq original, long offset) {
            this.seq = original;
            this.offset = offset;
            this.length = original.textSize() - offset;
        }

        public ArrayWrap(LSeq original, long offset, long length) {
            this.seq = original;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public long lookup(long index) {
            return seq.lookup(index + offset);
        }

        @Override
        public long textSize() {
            return length;
        }

        @Override
        public void set(long index, long value) {
            seq.set(index + offset, value);
        }

        @Override
        public long update(long index, long value) {
            return seq.update(index + offset, value);
        }

        @Override
        public String toString() {
            StringBuilder w = new StringBuilder();
            w.append("[");
            for (long i = 0; i < length; ++i) {
                if (i != 0)
                    w.append(", ");
                w.append(lookup(i));
            }
            w.append("]");
            return w.toString();
        }
    }

    public UInt32SAIS(LSeq T, int K) {
        this.T = T;
        this.N = T.textSize();
        this.K = K;
        this.bucketStart = new long[K];
        LS = new RSBitVector(N);
    }

    public static UInt32Array SAIS(LSeq T, int K) {
        UInt32Array SA = new UInt32Array(T.textSize());
        SAIS(T, SA, K);
        return SA;
    }

    public static LSeq SAIS(LSeq T, LSeq SA, int K) {
        UInt32SAIS sais = new UInt32SAIS(T, K);
        sais.SAIS(SA);
        return SA;
    }

    public void SAIS(LSeq SA) {

        // initialize the suffix array
        for (long i = 0; i < N; ++i)
            SA.set(i, 0);

        LS.setBit(SType, N - 1); // the sentinel 
        LS.setBit(LType, N - 2);

        // set the type of each character
        for (long i = N - 2; i > 0; --i) {
            long x = T.lookup(i);
            long y = T.lookup(i - 1);
            if (x < y)
                LS.setBit(LType, i - 1);
            else if (x > y)
                LS.setBit(SType, i - 1);
            else
                LS.setBit(LS.get(i), i - 1);
        }

        // Initialize the buckets. 
        // A bucket is a container of the suffixes sharing the same first character
        Arrays.fill(bucketStart, 0);
        // Compute the size of each bucket
        for (int i = 0; i < N; ++i) {
            ++bucketStart[(int) T.lookup(i)];
        }
        // Accumulate the character counts. The bucketStart holds the pointers to beginning of the buckets in SA
        for (int i = 1; i < bucketStart.length; ++i) {
            bucketStart[i] += bucketStart[i - 1];
        }

        // Step 1: reduce the problem by at least 1/2 
        // Sort all the S-substrings

        // Find LMS characters
        long[] cursorInBucket = Arrays.copyOf(bucketStart, bucketStart.length);
        for (int i = 1; i < N; ++i) {
            if (isLMS(i))
                SA.set(--cursorInBucket[(int) T.lookup(i)], i);
        }

        // Induced sorting LMS prefixes
        induceSA(SA);

        int numLMS = 0;
        // Compact all the sorted substrings into the first M items of SA
        // 2*M must be not larger than N 
        for (long i = 0; i < N; ++i) {
            if (isLMS(SA.lookup(i)))
                SA.set(numLMS++, SA.lookup(i));
        }

        // Initialize the name array buffer
        for (long i = numLMS; i < N; ++i)
            SA.set(i, 0);

        // Find the lexicographic names of the LMS substrings
        int name = 1;
        SA.set(numLMS + (SA.lookup(0) >> 1), name++);
        for (long i = 1; i < numLMS; ++i) {
            final long prev = SA.lookup(i - 1);
            final long current = SA.lookup(i);
            if (!isEqualLMS_substr(T, prev, current)) {
                name++;
            }
            SA.set(numLMS + (current >> 1), name - 1);
        }

        for (long i = N - 1, j = N - 1; i >= numLMS; --i) {
            if (SA.lookup(i) != 0)
                SA.set(j--, SA.lookup(i) - 1);
        }

        // Step 2: solve the reduced problem
        // Create SA1, a view of SA[0, numLMS-1]
        LSeq SA1 = new ArrayWrap(SA, 0, numLMS);
        LSeq T1 = new ArrayWrap(SA, N - numLMS, numLMS);
        if (name <= numLMS) {
            new UInt32SAIS(T1, name - 1).SAIS(SA1);
        }
        else {
            // When all LMS substrings have unique names
            for (long i = 0; i < numLMS; i++)
                SA1.set(T1.lookup(i), i);
        }

        // Step 3: Induce SA from SA1
        for (long i = 1, j = 0; i < N; ++i) {
            if (isLMS(i))
                T1.set(j++, i); // get p1
        }
        // get index in T 
        for (long i = 0; i < numLMS; ++i) {
            SA1.set(i, T1.lookup(SA1.lookup(i)));
        }

        System.arraycopy(bucketStart, 0, cursorInBucket, 0, bucketStart.length);
        // init SA[N1 .. N-1]
        for (long i = numLMS; i < N; ++i) {
            SA.set(--cursorInBucket[(int) T.lookup(SA1.lookup(i))], SA1.lookup(i));
        }
        SA.set(0, T.textSize() - 1);

        for (int i = numLMS - 1; i > 0; --i) {
            while (cursorInBucket[i] > bucketStart[i - 1])
                SA.set(--cursorInBucket[i], 0);
        }

        induceSA(SA);

    }

    //    private void findStartOfBuckets() {
    //        initBuckets();
    //        // compute the start of the buckets
    //        int sum = 0;
    //        for (int i = 0; i < K; ++i) {
    //            sum += bucketStart[i];
    //            bucketStart[i] = sum - bucketStart[i];
    //        }
    //    }
    //
    //    private void findEndOfBuckets() {
    //        initBuckets();
    //        // compute the end of the buckets
    //        long sum = 0;
    //        for (int i = 0; i < K; ++i) {
    //            sum += bucketStart[i];
    //            bucketStart[i] = sum;
    //        }
    //    }
    //
    //    private void initBuckets() {
    //        // initialize buckets
    //        for (int i = 0; i < K; ++i) {
    //            bucketStart[i] = 0;
    //        }
    //        // compute the size of each bucket
    //        for (int i = 0; i < N; ++i) {
    //            ++bucketStart[(int) T.lookup(i)];
    //        }
    //    }

    boolean isLMS(long pos) {
        return LS.get(pos) == SType && LS.get(pos - 1) == LType;
    }

    private void induceSA(LSeq SA) {
        long[] cursorInBucket = Arrays.copyOf(bucketStart, bucketStart.length);

        // induce left
        for (long i = 0; i < N; ++i) {
            long si = SA.lookup(i);
            if (si == 0)
                continue;
            if (LS.get(si - 1) == LType)
                SA.set(cursorInBucket[(int) T.lookup(si - 1) - 1]++, si - 1);
        }

        // induce right
        System.arraycopy(bucketStart, 0, cursorInBucket, 0, bucketStart.length);
        for (long i = N - 1; i >= 0; --i) {
            long si = SA.lookup(i);
            if (si == 0)
                continue;
            else if (LS.get(si - 1) == SType)
                SA.set(--cursorInBucket[(int) T.lookup(si - 1)], si - 1);
        }
    }

    boolean isEqualLMS_substr(LSeq T, long pos1, long pos2) {
        boolean prevLS = SType;
        for (; pos1 < N && pos2 < N; ++pos1, ++pos2) {
            if (T.lookup(pos1) == T.lookup(pos2) && LS.get(pos1) == LS.get(pos2)) {
                if (prevLS == LType && LS.get(pos1) == SType)
                    return true; // equal LMS substring
                prevLS = LS.get(pos1);
                continue;
            }
            else
                return false;
        }
        return false;
    }

}
