package samaya.toolbox

import ky.korins.blake3.{Blake3, Hasher => Blake3Hasher}
import samaya.config.ConfigHolder

import java.security.MessageDigest

//todo: Make configurable: We still need to support blake
object Crypto {
  trait Hasher {
    def update(input: Array[Byte]): Unit
    def finalize(len:Int): Array[Byte]
  }

  def newHasher(): Crypto.Hasher = {
    val version = ConfigHolder.base.options.get("version")
      .orElse(ConfigHolder.main.options.get("version"))

    if(version.isEmpty){
      Blake3Crypto.newHasher()
    } else if(version.get.contains("0")){
      Blake3Crypto.newHasher()
    } else {
      Sha2Crypto.newHasher()
    }
  }
}

object Sha2Crypto {
  case class MDHasher(hasher:MessageDigest) extends Crypto.Hasher {
    override def update(input: Array[Byte]): Unit = {
      hasher.update(input)
    }

    override def finalize(len: Int): Array[Byte] = {
      val full = hasher.digest()
      if(full.length == len) return full
      val trimmed = new Array[Byte](len);
      full.copyToArray(trimmed,0,len)
      trimmed
    }

    override def toString: String = hasher.toString
  }

  def newHasher(): Crypto.Hasher = MDHasher(MessageDigest.getInstance("SHA-256"))

}

object Blake3Crypto {
  case class BlakeHasher(hasher:Blake3Hasher) extends Crypto.Hasher{
    override def update(input: Array[Byte]): Unit = hasher.update(input)
    override def finalize(len: Int): Array[Byte] = hasher.done(len)
    override def toString: String = hasher.toString

  }

  def newHasher(): Crypto.Hasher = BlakeHasher(Blake3.newHasher())
}