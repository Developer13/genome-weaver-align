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
// FMIndexOnGenome.java
// Since: 2011/04/28
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import java.io.IOException;

import org.utgenome.UTGBException;
import org.utgenome.weaver.align.SequenceBoundary.PosOnGenome;
import org.utgenome.weaver.align.record.AlignmentRecord;
import org.xerial.lens.SilkLens;
import org.xerial.util.ObjectHandler;
import org.xerial.util.log.Logger;

public class FMIndexOnGenome
{
    private static Logger           _logger = Logger.getLogger(FMIndexOnGenome.class);

    public final FMIndex            forwardIndex;
    public final FMIndex            reverseIndex;
    private final SparseSuffixArray forwardSA;
    private final SparseSuffixArray backwardSA;
    private final SequenceBoundary  index;

    private final long              N;
    private final int               K;

    public FMIndexOnGenome(String fastaFilePrefix) throws UTGBException, IOException {

        _logger.info("Preparing FM-indexes");
        BWTFiles forwardDB = new BWTFiles(fastaFilePrefix, Strand.FORWARD);
        BWTFiles backwardDB = new BWTFiles(fastaFilePrefix, Strand.REVERSE);

        // Load the boundary information of the concatenated chr sequences 
        index = SequenceBoundary.loadSilk(forwardDB.pacIndex());
        N = index.totalSize;
        K = ACGT.values().length;

        // Load sparse suffix arrays
        BWAlign._logger.info("Loading sparse suffix arrays");
        forwardSA = SparseSuffixArray.loadFrom(forwardDB.sparseSuffixArray());
        backwardSA = SparseSuffixArray.loadFrom(backwardDB.sparseSuffixArray());

        ACGTSequence seqF = ACGTSequence.loadFrom(forwardDB.bwt());
        ACGTSequence seqR = ACGTSequence.loadFrom(backwardDB.bwt());
        int windowSize = 64;
        forwardIndex = new FMIndexOnOccTable(seqF, windowSize);
        reverseIndex = new FMIndexOnOccTable(seqR, windowSize);
        _logger.info("done.");
    }

    public long textSize() {
        return N;
    }

    public SuffixInterval forwardSearch(Strand strand, ACGT nextBase, SuffixInterval si) {
        FMIndex fm = strand == Strand.FORWARD ? reverseIndex : forwardIndex;
        return fm.backwardSearch(nextBase, si);
    }

    public SuffixInterval backwardSearch(Strand strand, ACGT nextBase, SuffixInterval si) {
        FMIndex fm = strand == Strand.FORWARD ? forwardIndex : reverseIndex;
        return fm.backwardSearch(nextBase, si);
    }

    public SuffixInterval backwardSearch(ACGT nextBase, SuffixInterval si) {
        return forwardIndex.backwardSearch(nextBase, si);
    }

    public void bidirectionalSearch(Strand strand, ACGT nextBase, SuffixInterval forwardSi, SuffixInterval backwardSi) {
        FMIndex fm = strand == Strand.FORWARD ? reverseIndex : forwardIndex;
        SuffixInterval f = fm.backwardSearch(nextBase, forwardSi);
        long x = 0;
        for (ACGT ch : ACGT.values()) {
            if (ch.code < nextBase.code) {
                x += fm.count(ch, forwardSi.lowerBound, forwardSi.upperBound + 1);
            }
        }
        long y = fm.count(nextBase, forwardSi.lowerBound, forwardSi.upperBound + 1);
        SuffixInterval b = new SuffixInterval(backwardSi.lowerBound + x, backwardSi.lowerBound + x + y);

    }

    public long toForwardSequenceIndex(long saIndex, Strand strand) {
        long pos = -1;
        switch (strand) {
        case FORWARD:
            long sa = backwardSA.get(saIndex, reverseIndex);
            pos = reverseIndex.textSize() - sa;
            break;
        case REVERSE:
            pos = forwardSA.get(saIndex, forwardIndex);
            break;
        }
        return pos;
    }

    public PosOnGenome toGenomeCoordinate(long saIndex, long querySize, Strand strand) throws UTGBException {
        long pos = toForwardSequenceIndex(saIndex, strand);
        if (strand == Strand.FORWARD) {
            pos -= querySize;
        }
        pos += 1;
        if (pos != -1) {
            PosOnGenome p = index.translate(pos, strand);
            return p;
        }
        return null;
    }

    public void toGenomeCoordinate(AlignmentSA result, ObjectHandler<AlignmentRecord> reporter) throws Exception {
        if (_logger.isTraceEnabled())
            _logger.info(SilkLens.toSilk("alignment", result));

        final long querySize = result.common.query.textSize();

        for (long i = result.suffixInterval.lowerBound; i < result.suffixInterval.upperBound; ++i) {
            PosOnGenome p = toGenomeCoordinate(i, querySize, result.strand);
            if (p != null) {
                AlignmentRecord rec = new AlignmentRecord();
                rec.chr = p.chr;
                rec.start = p.pos;
                rec.strand = result.strand;
                rec.score = result.alignmentScore;
                rec.numMismatches = result.numMismatches;
                // workaround for Picard tools, which cannot accept base character other than ACGT 
                rec.querySeq = result.strand == Strand.FORWARD ? result.common.query.toString() : result.common.query
                        .reverse().toString();
                rec.readName = result.common.queryName;
                rec.end = p.pos + result.wordIndex;
                rec.setCIGAR(result.cigar().toCIGARString());
                reporter.handle(rec);
            }
        }

    }
}
