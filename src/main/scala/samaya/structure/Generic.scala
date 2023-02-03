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

  def canEqual(other: Any): Boolean = other.isInstanceOf[Generic]
  override def equals(other: Any): Boolean = other match {
    case that: Generic =>
      (that canEqual this) &&
        name == that.name &&
        phantom == that.phantom &&
        capabilities == that.capabilities &&
        index == that.index
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(name, phantom, capabilities, index)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}