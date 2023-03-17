package samaya.types

import samaya.structure.types.Hash

import java.io.InputStream

trait InputSource extends GeneralSource with ContentAddressable{
  def location:Directory
  def content:InputStream
  override lazy val hash:Hash = Hash.fromInputSource(this)
}
