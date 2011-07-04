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
// FMIndexOnOccTableTest.java
// Since: 2011/05/16
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;
import org.utgenome.gwt.utgb.client.bio.IUPAC;
import org.utgenome.util.TestHelper;
import org.utgenome.weaver.GenomeWeaver;
import org.xerial.util.log.Logger;

public class FMIndexOnOccTableTest
{
    private static Logger _logger = Logger.getLogger(FMIndexOnOccTableTest.class);

    @Test
    public void backwardSearch() throws Exception {
        File fastaArchive = TestHelper.createTempFileFrom(BWTTest.class, "test.fa", new File("target", "test.fa"));
        GenomeWeaver.execute(String.format("bwt %s", fastaArchive));

        BWTFiles db = new BWTFiles(fastaArchive.getPath(), Strand.FORWARD);
        IUPACSequence bwt = IUPACSequence.loadFrom(db.bwt());
        FMIndex fmIndex = new FMIndexOnOccTable(bwt, 32);

        SuffixInterval si = fmIndex.backwardSearch(IUPAC.T, new SuffixInterval(0, fmIndex.textSize() - 1));
        _logger.info("suffix interval: " + si);
        assertEquals(new SuffixInterval(10, 15), si);

        si = fmIndex.backwardSearch(IUPAC.A, si);
        _logger.info("suffix interval: " + si);
        assertEquals(new SuffixInterval(5, 9), si);

        si = fmIndex.backwardSearch(IUPAC.T, si);
        _logger.info("suffix interval: " + si);
        assertEquals(new SuffixInterval(14, 15), si);

        si = fmIndex.backwardSearch(IUPAC.A, si);
        _logger.info("suffix interval: " + si);
        assertEquals(new SuffixInterval(9, 9), si);

        si = fmIndex.backwardSearch(IUPAC.A, si);
        _logger.info("suffix interval: " + si);
        assertEquals(new SuffixInterval(4, 4), si);

    }
}