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
// WeaverAlignTest.java
// Since: 2011/12/08
//
// $URL$ 
// $Author$
//--------------------------------------
package org.utgenome.weaver.align.strategy.weaver

import org.junit._
import Assert._
import org.utgenome.weaver.align._
import org.utgenome.weaver.read._
import org.xerial.util.log.Logger


class WeaverAlignTest {
  object Fixture {

    val config: AlignmentConfig = new AlignmentConfig()
    val ref: ACGTSequence = new ACGTSequence("AAGCCTAGTTTCCTTG")
    val fmIndex: FMIndexOnGenome = FMIndexOnGenome.buildFromSequence("seq", ref)

    @BeforeClass
    def setUp() {
      //fmIndex = FMIndexOnGenome.buildFromSequence("seq", ref);
      config.k = 2;
    }
  }

  //type Alignment = org.utgenome.weaver.align.strategy.Alignment
  val _logger: Logger = Logger.getLogger(classOf[WeaverAlignTest])
  val p = Fixture

  def align(query: String): Alignment = {
    align(new ACGTSequence(query))
  }

  def align(q: ACGTSequence): Alignment = {
    val aln: WeaverAlign = new WeaverAlign(p.fmIndex, p.ref, p.config);
    aln.align(FASTQRead("read", new CompactDNASequence(q), null))
  }

  @Test def hello() {
    _logger debug("hello world")
  }

  @Test
  def forwardExact() {
    val a: Alignment = align("GCCTAGT");
    a match {
      case f: UniquelyMapped => {
        assertEquals("7M", f.cigar.toString);
        assertEquals(3, f.start);
        assertEquals(Strand.FORWARD, f.strand);
        assertEquals(0, f.numMismatches)
      }
      case _ => throw new Exception("cannot reach here")
    }
  }

  @Test
  def reverseExact() {
    val a: Alignment = align(new ACGTSequence("GCCTAGT").reverseComplement());
    a match {
      case f: UniquelyMapped => {
        assertEquals("7M", f.cigar.toString);
        assertEquals(3, f.start);
        assertEquals(Strand.REVERSE, f.strand);
        assertEquals(0, f.numMismatches)
      }
      case _ => throw new Exception("cannot reach here")
    }
  }
  @Test
  def oneDeletion() {
    val a: Alignment = align("AAGCCTGTTT");
    a match {
      case m: UniquelyMapped => {
        assertEquals(1, m.start);
        assertEquals(Strand.FORWARD, m.strand);
        assertEquals(1, m.numMismatches);
        assertEquals("6M1D4M", m.cigar.toString);
      }
      case _ => throw new Exception("cannot reach here")
    }
  }

  @Test
  def forwardCursor() {
    val c = new ForwardCursor(new ReadRange(Strand.FORWARD, 0, 5), 0)
    _logger.debug(c)

    var cc: Option[ReadCursor] = Some(c)
    for (i <- 0 until 5) {
      cc = cc.get.next
      _logger.debug(cc match { case Some(x) => x; case None => "None" });
    }

  }

}
