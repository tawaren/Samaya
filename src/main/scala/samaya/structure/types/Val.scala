package samaya.structure.types

import samaya.structure.types.UnknownSourceId.origin

trait Val extends Ref {
  def id:Id
  def target:SourceId
  def withAdaptedSource(src:SourceId):Val
}

object Val {
  def unknown(name: String, src: SourceId):Val = new Unknown(name,src)
  def apply(id:Id, origin:SourceId, pos:Int):Val = BaseVal(id,origin,pos)(id.src)

  case class BaseVal(override val id:Id, target:SourceId, pos:Int)(override val src: SourceId) extends Val {
    override def name: String = id.name
    override def withAdaptedSource(src: SourceId): Val = BaseVal(id, target, pos)(src)
  }

  class Unknown(override val name: String, override val src: SourceId) extends Val{
    override def target: SourceId = UnknownSourceId
    override def withAdaptedSource(src: SourceId): Val = new Unknown(name, src)
  }
}