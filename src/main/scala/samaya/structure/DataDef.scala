package samaya.structure

import java.io.DataOutputStream

import samaya.structure.types.{Accessibility, Capability, Permission}

trait DataDef extends TypeParameterized with ModuleEntry{
  def index:Int
  def name:String
  def attributes:Seq[Attribute]
  def accessibility:Map[Permission,Accessibility]
  def generics:Seq[Generic]
  def generic(index:Int):Option[Generic] = generics.find(gi => gi.index == index)
  def constructors:Seq[Constructor]
  def constructor(tag:Int):Option[Constructor]= constructors.find(c => c.tag == tag)
  def capabilities:Set[Capability]
  def external:Option[Short]
}

object DataDef {

  def serializeDefault(out: DataOutputStream): Unit = {
    //6B = 1B capabilities + 1B generics +3B imports (1B modules, 1B Errors, 1B Types) +1B Constructors
    // In default all these bytes are 0
    out.writeInt(0)   //4 Zero Bytes
    out.writeShort(0) //2 Zero Bytes
  }
}
