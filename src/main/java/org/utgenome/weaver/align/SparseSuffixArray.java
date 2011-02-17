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
// SparseSuffixArray.java
// Since: 2011/02/12
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 
 * 
 * S(i) = S(X^{(j)}(i)) + j
 * 
 * X(i) = C(BWT[i]) + O(BWT[i], i)
 * 
 * @author leo
 * 
 */
public class SparseSuffixArray
{
    private final LIntArray sparseSA;
    private final long      N;
    private final int       L;

    private SparseSuffixArray(LIntArray sparseSA, long N, int L) {
        this.sparseSA = sparseSA;
        this.N = N;
        this.L = L;
    }

    public static SparseSuffixArray createFromWaveletBWT(WaveletArray W, int suffixInterval) {
        FMIndex F = new FMIndex(W);

        final long N = W.textSize();
        final long sparseSA_length = (N + suffixInterval - 1) / suffixInterval;
        LIntArray sparseSA = new LIntArray(sparseSA_length);

        long sa = 0;
        long saIndex = N - 1;
        for (long i = 0; i < N; ++i) {
            if (saIndex % suffixInterval == 0) {
                sparseSA.set(saIndex / suffixInterval, sa);
            }
            --sa;

            saIndex = F.suffixLink(saIndex);
        }

        return new SparseSuffixArray(sparseSA, N, suffixInterval);
    }

    public void saveTo(File f) throws IOException {
        DataOutputStream d = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try {
            saveTo(d);
        }
        finally {
            d.close();
        }
    }

    public void saveTo(DataOutputStream d) throws IOException {
        d.writeLong(N);
        d.writeInt(L);
        d.writeLong(sparseSA.size());
        for (int i = 0; i < sparseSA.size(); ++i) {
            d.writeInt((int) sparseSA.get(i));
        }
        d.flush();
    }

    public static SparseSuffixArray loadFrom(File f) throws IOException {
        DataInputStream d = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
        try {
            return SparseSuffixArray.loadFrom(d);
        }
        finally {
            d.close();
        }
    }

    public static SparseSuffixArray loadFrom(DataInputStream d) throws IOException {
        final long N = d.readLong();
        final int L = d.readInt();
        final long sparseSALength = d.readLong();
        LIntArray sparseSA = new LIntArray(sparseSALength);
        for (int i = 0; i < sparseSA.size(); ++i) {
            sparseSA.set(i, 1L | d.readInt());
        }
        return new SparseSuffixArray(sparseSA, N, L);
    }

    public static SparseSuffixArray buildFromSuffixArray(LIntArray SA, int L) {
        long sparseSA_length = (SA.size() + L - 1) / L;
        LIntArray sparseSA = new LIntArray(sparseSA_length);
        for (long i = 0; i < sparseSA_length; ++i) {
            sparseSA.set(i, SA.get(i * L));
        }
        return new SparseSuffixArray(sparseSA, SA.size(), L);
    }

    public long get(long index, FMIndex fmIndex) {
        long pos = index / L;
        long offset = index % L;

        if (pos > Integer.MAX_VALUE) {
            throw new ArrayIndexOutOfBoundsException("index: " + index);
        }

        if (offset == 0)
            return sparseSA.get(pos);

        long cursor = index;
        final long N = fmIndex.textSize();
        for (long j = 1; j <= N; j++) {
            cursor = fmIndex.suffixLink(cursor);
            if (cursor == 0) {
                return j - 1;
            }
            if (cursor % L == 0)
                return sparseSA.get(cursor / L) + j;
        }
        throw new IllegalStateException(String.format("cannot reach here: get(index:%d)", index));

    }
}
