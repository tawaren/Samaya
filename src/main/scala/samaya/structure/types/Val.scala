package samaya.structure.types

trait Val extends Ref {
  def id:Id
  def origin:SourceId
}

object Val {
  def unknown():Val = new Unknown()
  def apply(id:Id, origin:SourceId, pos:Int):Val = BaseVal(id,origin,pos)

  case class BaseVal(id:Id, origin:SourceId, pos:Int) extends Val
  class Unknown extends Val{
    override def id: Id = Id("unknown")
    override def origin: SourceId = UnknownSourceId
  }
}


