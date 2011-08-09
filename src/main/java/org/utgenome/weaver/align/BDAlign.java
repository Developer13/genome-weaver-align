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
// BDAlign.java
// Since: Jul 14, 2011
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import org.utgenome.UTGBException;
import org.utgenome.weaver.GenomeWeaverCommand;
import org.utgenome.weaver.align.record.AlignmentRecord;
import org.utgenome.weaver.align.record.RawRead;
import org.utgenome.weaver.align.record.ReadSequence;
import org.utgenome.weaver.align.record.ReadSequenceReader;
import org.utgenome.weaver.align.record.ReadSequenceReaderFactory;
import org.utgenome.weaver.align.strategy.BidirectionalCursor;
import org.utgenome.weaver.align.strategy.BidirectionalNFA;
import org.utgenome.weaver.parallel.Reporter;
import org.xerial.lens.SilkLens;
import org.xerial.util.ObjectHandlerBase;
import org.xerial.util.StopWatch;
import org.xerial.util.log.Logger;
import org.xerial.util.opt.Argument;
import org.xerial.util.opt.Option;

/**
 * Bi-directional BWT Aligner
 * 
 * @author leo
 * 
 */
public class BDAlign extends GenomeWeaverCommand
{

    private static Logger _logger = Logger.getLogger(BDAlign.class);

    @Override
    public String getOneLineDescription() {
        return "bi-directional bwt alignment";
    }

    @Argument(index = 0)
    private String  fastaFilePrefix;

    @Argument(index = 1)
    private String  readFile;

    @Option(symbol = "q", description = "query sequence")
    private String  query;

    @Option(symbol = "k", description = "maximum edit distances")
    private int     maximumEditDistances = 2;

    @Option(longName = "quiet", description = "quiet mode")
    private boolean quite                = false;

    @Override
    public void execute(String[] args) throws Exception {

        if (query == null && readFile == null)
            throw new UTGBException("no query is given");

        ReadSequenceReader reader = null;
        if (query != null) {
            _logger.info("query sequence: " + query);
            reader = ReadSequenceReaderFactory.singleQueryReader(query);
        }
        else if (readFile != null) {
            reader = ReadSequenceReaderFactory.createReader(readFile);
        }

        BWTFiles forwardDB = new BWTFiles(fastaFilePrefix, Strand.FORWARD);
        SequenceBoundary b = SequenceBoundary.loadSilk(forwardDB.pacIndex());

        AlignmentScoreConfig config = new AlignmentScoreConfig();
        config.maximumEditDistances = maximumEditDistances;

        query(fastaFilePrefix, reader, config);
    }

    public void query(String fastaFilePrefix, ReadSequenceReader readReader, final AlignmentScoreConfig config)
            throws Exception {
        final FMIndexOnGenome fmIndex = new FMIndexOnGenome(fastaFilePrefix);

        readReader.parse(new ObjectHandlerBase<RawRead>() {
            int       count = 0;
            StopWatch timer = new StopWatch();

            @Override
            public void handle(RawRead read) throws Exception {
                final ReadSequence r = (ReadSequence) read;
                BidirectionalNFA aligner = new BidirectionalNFA(fmIndex, new ACGTSequence(r.seq), config,
                        new Reporter() {
                            @Override
                            public void emit(Object result) throws UTGBException {
                                if (_logger.isDebugEnabled())
                                    _logger.debug(SilkLens.toSilk("result", result));

                                if (BidirectionalCursor.class.isInstance(result)) {
                                    BidirectionalCursor c = BidirectionalCursor.class.cast(result);
                                    AlignmentRecord aln = c.convert(r.name, fmIndex);
                                    if (!quite)
                                        System.out.println(aln.toSAMLine());
                                }

                            }
                        });

                aligner.align();
                count++;
                double time = timer.getElapsedTime();
                if (count % 10000 == 0) {
                    _logger.info("%,d reads are processed in %.2f sec. %,.0f reads/sec.", count, time, count / time);
                }
            }
        });

    }

}
