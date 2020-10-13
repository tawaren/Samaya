package samaya.structure.types

import java.io.{DataOutputStream, InputStream, OutputStream}
import java.math.BigInteger
import java.security.{DigestInputStream, DigestOutputStream, MessageDigest}
import java.util

import org.apache.commons.codec.binary.Hex
import com.rfksystems.blake2b.Blake2b
import samaya.types.{InputSource, OutputTarget}

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
  def fromBytes(data: Array[Byte]): Hash = Hash(data)
  def fromString(hash:String):Hash = Hash(Hex.decodeHex(hash))

  //Helper to directly Hash an InputStream
  def fromInputSource(input: InputSource): Hash = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
    val digestStream = new DigestInputStream(input.content, digest)
    while (digestStream.read() > -1) {}
    digestStream.close()
    fromBytes(digest.digest())
  }


  def writeAndHash(out:OutputTarget, writer:OutputStream => Unit):Hash =  {
    out.write(out => {
      val stream = new DigestOutputStream(out, MessageDigest.getInstance(Blake2b.BLAKE2_B_160))
      try {
        writer(stream)
      } finally {
        stream.close()
      }
      fromBytes(stream.getMessageDigest.digest())
    })
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
