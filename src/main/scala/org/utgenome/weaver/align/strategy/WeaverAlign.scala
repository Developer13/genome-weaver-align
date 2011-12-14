/*--------------------------------------------------------------------------
 *  Copyright 2011 Taro L. Saito
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
// genome-weaver project
//
// WeaverAlign.java
// Since: Dec 3, 2011
//
// $URL$ 
// $Author$
//--------------------------------------

package org.utgenome.weaver.align.strategy

import org.utgenome.weaver.align._
import org.utgenome.weaver.read._
import org.utgenome.weaver.parallel.Reporter
import org.utgenome.weaver.read._
import scala.collection.mutable.PriorityQueue

object WeaverAlign {

}

class Interval(val start: Int, val end: Int) {
  override def toString = "[%d,%d)".format(start, end)
}

trait Pivot {
  val pivot: Int
}

trait HasStrand {
  val strand: Strand
}

class ReadRange(val strand: Strand, start: Int, end: Int) extends Interval(start, end) with HasStrand {
  override def toString = "%s%s".format(strand.symbol, super[Interval].toString())
}

/**
 * Points the position of the current search within the read
 * @author leo
 *
 */
sealed abstract class ReadCursor(val readRange: ReadRange, val cursor: Int) {
  def processedBases: Int
  def currentIndex: Int
  def next: Option[ReadCursor]
  def strand = readRange.strand

  override def toString = "%d%s".format(cursor, readRange)
}

class ForwardCursor(readRange: ReadRange, cursor: Int) extends ReadCursor(readRange, cursor) {
  def processedBases = cursor - readRange.start
  def currentIndex = cursor
  def next = {
    val newCursor = cursor + 1
    if (newCursor >= readRange.end) None else Some(new ForwardCursor(readRange, newCursor))
  }
  override def toString = "F%s".format(super[ReadCursor].toString)
}

class BackwardCursor(readRange: ReadRange, cursor: Int) extends ReadCursor(readRange, cursor) {
  def processedBases = readRange.end - cursor
  def currentIndex = cursor - 1
  def next = {
    val newCursor = cursor - 1
    if (newCursor <= readRange.start) None else Some(new BackwardCursor(readRange, newCursor))
  }
  override def toString = "B%s".format(super[ReadCursor].toString)
}

class BidirectionalForwardCursor(readRange: ReadRange with Pivot, cursor: Int) extends ReadCursor(readRange, cursor) {
  def processedBases = cursor - readRange.pivot
  def currentIndex = cursor
  def next = {
    val newCursor = cursor + 1
    if (newCursor < readRange.end)
      Some(new BidirectionalForwardCursor(readRange, newCursor))
    else if (readRange.pivot > readRange.start)
      Some(new BackwardCursor(readRange, readRange.pivot))
    else
      None
  }
  override def toString = "BF%s/%d".format(super[ReadCursor].toString, readRange.pivot)
}

sealed trait Alignment
case class NoHit(read: Read) extends Alignment
case class TooManyNs(read: Read) extends Alignment
case class FMIndexHit(read: Read, si: SuffixInterval, strand: Strand, numMismatches: Int) extends Alignment

class WeaverAlign(fmIndex: FMIndexOnGenome, reference: ACGTSequence, config: AlignmentConfig) {

  trait Aligner {
    def align(): Alignment
  }

  def align(read: Read): Alignment = {
    val aligner: Aligner = read match {
      case a @ FASTARead(name, seq) => new SingleEndAligner(a)
      case b @ FASTQRead(name, seq, qual) => new SingleEndAligner(b)
      case p @ PairedEndRead(first, second) => new PairedEndAligner(p)
    }

    aligner.align()
  }

  class ReadBreakPoints(
    val strand: Strand,
    val si: SuffixInterval,
    val breakPoints: BitVector,
    val numMismatches: Int,
    val longestMatch: Range,
    val longestMatchSi: SuffixInterval);

  /**
   * Scan the longest unique match region in the query sequence by using FM-index
   * @param query
   * @param strand
   * @return
   */
  def quickScan(query: DNASequence, strand: Strand): ReadBreakPoints = {
    val qLen: Int = query.size.toInt
    val breakPoint = new BitVector(qLen)

    var numMismatches = 0
    var si = fmIndex.wholeSARange()
    var mark = 0
    var longestMatch: Range = null
    var longestMatchSi: SuffixInterval = null;

    var i = 0;
    for (i <- (0 until qLen)) {
      val ch = query(i);
      si = fmIndex.forwardSearch(strand, ch, si);
      if (si.isEmpty()) {
        breakPoint.set(i, true);
        numMismatches += 1
        if (longestMatch == null || longestMatch.length() < (i - mark)) {
          longestMatch = new Range(mark, i);
          longestMatchSi = si;
        }
        si = fmIndex.wholeSARange();
        mark = i + 1;
      }
    }
    if (longestMatch == null || longestMatch.length() < (i - mark)) {
      longestMatch = new Range(mark, i);
    }
    return new ReadBreakPoints(strand, si, breakPoint, numMismatches, longestMatch, longestMatchSi)
  }

  implicit def dnaToACGT(x: DNASequence): ACGTSequence = {
    x match { case a: CompactDNASequence => a.seq }
  }

  class SearchQueue extends PriorityQueue[Search] {

    def +=(a: Option[Search]): this.type = {
      a match {
        case Some(x) => this += x
        case None =>
      }
      this
    }

  }

  class SearchStat(val numFMSearch: Int = 0, val numCutOff: Int = 0, val numFiltered: Int = 0)

  class SingleEndAligner(read: SingleEnd) extends Aligner {
    private val m = read.length
    private val k = config.k

    private val queue = new SearchQueue

    val stat = new SearchStat

    def align(): Alignment = {
      // Check whether the read contains too many Ns
      def containsTooManyNs(r: SingleEnd) = { r.seq.count(ACGT.N, 0, r.length) > k }
      if (containsTooManyNs(read)) return TooManyNs(read)

      val q: Array[DNASequence] = Array(read.seq.replaceN_withA, read.seq.complement.replaceN_withA)

      // quick scan for k=0 
      {
        val scanF = quickScan(q(0), Strand.FORWARD)
        if (scanF.numMismatches == 0) return FMIndexHit(read, scanF.si, scanF.strand, 0)
        val scanR = quickScan(q(1), Strand.REVERSE)
        if (scanR.numMismatches == 0) return FMIndexHit(read, scanR.si, scanR.strand, 0)

        if (k == 0) return NoHit(read);

        def initialState(quickScan: ReadBreakPoints): Option[Search] = {
          if (scanF.numMismatches > k)
            return None;
          else if (scanF.longestMatch.start != 0 && scanF.longestMatch.start < m) {
            // bi-directional search
            return Some(new Search(new BidirectionalForwardCursor(new ReadRange(quickScan.strand, 0, m) with Pivot { val pivot = quickScan.longestMatch.start }, quickScan.longestMatch.start), quickScan.numMismatches))
          } else {
            // single-direction search
            return Some(new Search(new ForwardCursor(new ReadRange(quickScan.strand, 0, m), 0), quickScan.numMismatches));
          }
        }

        queue += initialState(scanF)
        queue += initialState(scanR)
      }

      val queryMask = Array(new QueryMask(q(0)), new QueryMask(q(1)))

      while (!queue.isEmpty) {
        val c: Search = queue.dequeue

      }

      NoHit(read)
    }
  }
  class Search(val cursor: ReadCursor, val priority: Int) {
    def score: Int = 0

  }

  implicit val searchOrder = new Ordering[Search] {
    def compare(o1: Search, o2: Search): Int = {
      var diff = 0
      diff = o1.priority - o2.priority
      if (diff == 0)
        diff = -(o1.score - o2.score);
      if (diff == 0)
        diff = -(o1.cursor.processedBases - o2.cursor.processedBases);
      return diff;
    }
  }

  class PairedEndAligner(pe: PairedEndRead) extends Aligner {
    def align(): Alignment = {

      NoHit(pe)
    }

  }

}
