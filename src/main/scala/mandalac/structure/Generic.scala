package mandalac.structure

import java.io.DataOutputStream

import mandalac.structure.meta.GenericAttribute
import mandalac.structure.types.Capability

trait Generic extends Indexed {
  def name:String
  def pos:Int
  //undefined on data
  //todo: make sure is checked on function
  def protection:Boolean
  def phantom:Boolean
  def capabilities:Set[Capability]
  def attributes:Seq[GenericAttribute]

  def serialize(out: DataOutputStream): Unit ={
    if(phantom) {
      //write phantom
      out.writeByte(0)
    } else {
      //write physical
      out.writeByte(1)
      val caps = capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
      out.writeByte(caps)
    }
  }

  override def index: Int = pos

}
