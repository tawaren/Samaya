package samaya.types

import samaya.structure.types.Hash

import java.io.{InputStream, OutputStream}

trait InputSource extends GeneralSource with ContentAddressable{
  def location:Directory
  def read[T](reader:InputStream => T):T
  def readAllBytes():Array[Byte] = read(_.readAllBytes())
  override lazy val hash:Hash = Hash.fromInputSource(this)
  def copyTo(target:OutputTarget):Unit = {
    target.write(out => read(in => {
      val buffer = new Array[Byte](8 * 1024)
      var length = in.read(buffer)
      while (length >= 0) {
        out.write(buffer, 0, length)
        length = in.read(buffer)
      }
    }))
  }
}
