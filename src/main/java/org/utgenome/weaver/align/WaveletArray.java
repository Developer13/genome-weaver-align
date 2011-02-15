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
// WaveletArray.java
// Since: 2011/02/15
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import java.util.ArrayList;

public class WaveletArray
{
    private ArrayList<BitVector> bitVector       = new ArrayList<BitVector>();
    private BitVector            occ;

    private long                 alphabetSize    = 0;
    private long                 alphabetBitSize = 0;
    private long                 size            = 0;

    public WaveletArray() {

    }

    public static long log2(long x) {
        if (x == 0)
            return 0;
        x--;
        long bit_num = 0;
        while ((x >>> bit_num) > 0) {
            ++bit_num;
        }
        return bit_num;
    }

    public void init(LSeq input, long K) {
        alphabetSize = K;
        alphabetBitSize = log2(K);

        size = input.textSize();

        setArray(input);
        setOccs(input);
    }

    protected static long prefixCode(long x, long len, long bitNum) {
        return x >>> (bitNum - len);
    }

    protected static long getMSB(long x, long pos, long len) {
        return (x >>> (len - (pos + 1))) & 1L;
    }

    protected void setArray(LSeq input) {
        if (alphabetSize == 0)
            return;
        bitVector = new ArrayList<BitVector>((int) alphabetBitSize);

        ArrayList<long[]> beg_poses = getBegPoses(input, alphabetBitSize);

        for (long i = 0; i < input.textSize(); ++i) {
            long c = input.lookup(i);
            for (int j = 0; j < (int) alphabetBitSize; ++j) {
                long prefix_code = prefixCode(c, j, alphabetBitSize);
                long bit_pos = beg_poses.get(j)[(int) prefix_code]++;
                bitVector.get(j).setBit(getMSB(c, j, alphabetBitSize) > 0, bit_pos);
            }
        }
    }

    protected ArrayList<long[]> getBegPoses(LSeq input, long alphabetBitNum) {
        ArrayList<long[]> beg_poses = new ArrayList<long[]>((int) alphabetBitNum);
        for (long i = 0; i < beg_poses.size(); ++i) {
            long[] v = new long[1 << i];
            for (int k = 0; k < v.length; ++k)
                v[k] = 0L;
            beg_poses.add(v);
        }

        for (long i = 0; i < input.textSize(); ++i) {
            long c = input.lookup(i);
            for (int j = 0; j < alphabetBitNum; ++j) {
                beg_poses.get(j)[(int) prefixCode(c, j, alphabetBitNum)]++;
            }
        }

        for (long i = 0; i < beg_poses.size(); ++i) {
            long sum = 0;
            long[] beg_poses_level = beg_poses.get((int) i);
            for (int j = 0; j < beg_poses_level.length; ++j) {
                long num = beg_poses_level[j];
                beg_poses_level[j] = sum;
                sum += num;
            }
        }

        return beg_poses;
    }

    protected void setOccs(LSeq input) {
        long[] counts = new long[(int) alphabetSize];
        for (long i = 0; i < input.textSize(); ++i) {
            counts[(int) input.lookup(i)]++;
        }

        occ = new BitVector(input.textSize() + alphabetSize + 1);
        long sum = 0;
        for (int i = 0; i < counts.length; ++i) {
            occ.setBit(true, sum);
            sum += counts[i] + 1;
        }
        occ.setBit(true, sum);
    }

    public long rank(long c, long pos) {
        Rank r = rankAll(c, pos);
        return r.rank;
    }

    private static class Rank
    {
        public final long rank;
        public final long rankLessThan;
        public final long rankMoreThan;

        public Rank(long rank, long rankLessThan, long rankMoreThan) {
            this.rank = rank;
            this.rankLessThan = rankLessThan;
            this.rankMoreThan = rankMoreThan;
        }
    }

    public Rank rankAll(long c, long pos) {
        if (c >= alphabetSize) {
            return new Rank(0, 0, 0);
        }
        if (pos >= size) {
            pos = size;
        }
        long beg_node = 0;
        long end_node = size;
        long rankLessThan = 0;
        long rankMoreThan = 0;

        for (int i = 0; i < bitVector.size() && beg_node < end_node; ++i) {
            BitVector ba = bitVector.get(i);
            long beg_node_zero = ba.rank(false, beg_node);
            long beg_node_one = beg_node - beg_node_zero;
            long end_node_zero = ba.rank(false, end_node);
            long boundary = beg_node + end_node_zero - beg_node_zero;
            long bit = getMSB(c, i, bitVector.size());
            if (bit == 0) {
                rankMoreThan += ba.rank(true, pos) - beg_node_one;
                pos = beg_node + ba.rank(false, pos) - beg_node_zero;
                end_node = boundary;
            }
            else {
                rankLessThan += ba.rank(false, pos) - beg_node_zero;
                pos = boundary + ba.rank(true, pos) - (beg_node - beg_node_zero);
                beg_node = boundary;
            }
        }
        long rank = pos - beg_node;
        return new Rank(rank, rankLessThan, rankMoreThan);
    }

}
