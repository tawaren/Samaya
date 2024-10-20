package samaya.structure.types

import java.io.{FilterInputStream, FilterOutputStream, InputStream, OutputStream}
import java.util
import samaya.types.InputSource
import org.apache.commons.codec.binary.Hex
import samaya.toolbox.Crypto

case class Hash(data:Array[Byte]) {
  override def toString:String = Hex.encodeHexString(data)
  def canEqual(other: Any): Boolean = other.isInstanceOf[Hash]
  override def equals(other: Any): Boolean = other match {
    case that@Hash(otherData) => (that canEqual this) && (data sameElements otherData)
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(data)
    state.map(util.Arrays.hashCode).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object Hash {
  val byteLen:Int = 20
  val charLen:Int = byteLen*2

  def fromBytes(data: Array[Byte]): Hash = Hash(data)
  def fromString(hash:String):Hash = Hash(Hex.decodeHex(hash))

  //Helper to directly Hash an InputStream
  def fromInputSource(input: InputSource): Hash = {
    val digest = Crypto.newHasher()
    input.read { in =>
      val digestStream = new HashedInputStream(in, digest)
      while (digestStream.read() > -1) {}
      digestStream.close()
      fromBytes(digest.finalize(Hash.byteLen))
    }
  }

  implicit val byteOrdering:Ordering[Hash] = new Ordering[Hash] {
    def compare(ah:Hash, bh: Hash): Int = {
      val a = ah.data
      val b = bh.data
      if (a eq null) {
        if (b eq null) 0
        else -1
      }
      else if (b eq null) 1
      else {
        val L = math.min(a.length, b.length)
        var i = 0
        while (i < L) {
          if (a(i) < b(i)) return -1
          else if (b(i) < a(i)) return 1
          i += 1
        }
        if (L < b.length) -1
        else if (L < a.length) 1
        else 0
      }
    }
  }
}

class HashedInputStream(stream: InputStream, digest:Crypto.Hasher) extends FilterInputStream(stream) {
  override def read: Int = {
    val ch = in.read
    if (ch != -1) digest.update(Array(ch.toByte))
    ch
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    val result = in.read(b, off, len)
    if (result != -1) digest.update(b.slice(off, off+result))
    result
  }
  override def toString: String = "[Digest Input Stream] " + digest
}

class HashedOutputStream(stream: OutputStream, digest:Crypto.Hasher) extends FilterOutputStream(stream) {

  override def write(b: Int): Unit = {
    out.write(b)
    digest.update(Array(b.toByte))
  }

  override def write(b: Array[Byte], off: Int, len: Int): Unit = {
    out.write(b, off, len)
    digest.update(b.slice(off,off+len))
  }

  override def toString: String = "[Digest Output Stream] " + digest
}
