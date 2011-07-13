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
// PackFASTA.java
// Since: 2011/07/13
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.utgenome.format.fasta.CompactFASTA;
import org.utgenome.format.fasta.FASTAPullParser;
import org.utgenome.weaver.align.SequenceBoundary.SequenceIndex;
import org.xerial.lens.SilkLens;
import org.xerial.silk.SilkWriter;
import org.xerial.util.log.Logger;
import org.xerial.util.opt.Argument;

public class PackFasta extends GenomeWeaverCommand
{
    private static Logger _logger = Logger.getLogger(PackFasta.class);

    @Override
    public String getOneLineDescription() {
        return "Create ACGT 3-bit index of FASTA sequences";
    }

    @Argument(index = 0, required = true)
    private String fastaFile;

    @Override
    public void execute(String[] args) throws Exception {

        encode(fastaFile);
    }

    public static void encode(String fastaFile) throws IOException {
        encodeFASTA(fastaFile);

        BWTFiles forwardDB = new BWTFiles(fastaFile, Strand.FORWARD);
        BWTFiles reverseDB = new BWTFiles(fastaFile, Strand.REVERSE);
        reverse(forwardDB, reverseDB);
    }

    protected static void encodeFASTA(String fastaFile) throws IOException {
        BWTFiles forwardDB = new BWTFiles(fastaFile, Strand.FORWARD);
        _logger.info("input FASTA file: " + fastaFile);

        long totalSize = -1;
        {
            // Read the input FASTA file, then encode the sequences using the IUPAC code
            SilkWriter indexOut = new SilkWriter(new BufferedWriter(new FileWriter(forwardDB.pacIndex())));
            ACGTSequence packed = new ACGTSequence();
            FASTAPullParser fasta = new FASTAPullParser(new File(fastaFile));
            long lineCount = 1;
            long offset = 0;
            for (String desc; (desc = fasta.nextDescriptionLine()) != null; lineCount++) {
                String seqName = CompactFASTA.pickSequenceName(desc);
                _logger.info(String.format("reading %s", seqName));
                for (String seq; (seq = fasta.nextSequenceLine()) != null; lineCount++) {
                    seq = seq.trim();
                    for (int i = 0; i < seq.length(); ++i) {
                        // 'A' .. 'Z'
                        packed.append(seq.charAt(i));
                    }
                }
                long pos = packed.textSize();
                long sequenceSize = pos - offset;
                SequenceIndex index = new SequenceIndex(seqName, desc, sequenceSize, offset);
                indexOut.leafObject("index", index);
                if (_logger.isDebugEnabled())
                    _logger.debug("\n" + SilkLens.toSilk("index", index));
                offset = pos;
            }
            totalSize = packed.textSize();
            _logger.info(String.format("total num bases: %,d", totalSize));
            indexOut.leaf("total size", totalSize);
            indexOut.close();

            packed.saveTo(forwardDB.pac());
        }

    }

    protected static void reverse(BWTFiles forwardDB, BWTFiles reverseDB) throws IOException {
        // Reverse the IUPAC sequence
        ACGTSequence forwardSeq = ACGTSequence.loadFrom(forwardDB.pac());
        _logger.info("Reverse the ACGT sequence %s to %s", forwardDB.pac(), reverseDB.pac());
        ACGTSequence reverseSeq = forwardSeq.reverse();
        reverseSeq.saveTo(reverseDB.pac());
    }

}
