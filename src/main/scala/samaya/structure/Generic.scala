package samaya.structure

import samaya.structure.types.{Capability, SourceId, Type}

trait Generic extends Indexed {
  def name:String
  def index: Int
  def phantom:Boolean
  def capabilities:Set[Capability]
  def attributes:Seq[Attribute]
  def src:SourceId

  def asType(src:SourceId):Type = Type.GenericType(capabilities,index)(src)
}