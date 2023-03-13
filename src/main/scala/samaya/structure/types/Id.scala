package samaya.structure.types

import samaya.structure.Attribute

class Id private (override val name:String)(override val src:SourceId) extends Ref {
  def canEqual(other: Any): Boolean = other.isInstanceOf[Id]

  //comparing / hashing only by name is on purpose as src is only met information
  override def equals(other: Any): Boolean = other match {
    case that: Id =>  (that canEqual this) && name == that.name
    case _ => false
  }

  override def hashCode(): Int = name.hashCode()

  override def toString = s"Id($name, $src)"
}

object Id {
  //Todo: can we make Id Generator and extract / reinite per entry
  //that would allow for pseudo stable id names independent of compilation order
  var counter = 0
  def nextNo():Int = {
    counter+=1
   counter
  }

  def apply(src:SourceId):Id = new Id("$#"+nextNo())(src)
  def apply(num:Int,src:SourceId):Id = new Id("$"+num)(src)

  def apply(name:String,src:SourceId):Id = new Id(name)(src)
  def apply(id:Id):Id = new Id(s"${id.name}#${nextNo()}")(id.src)

  //in case the source language allows $ it needs to call this before apply
  def escapeName(name:String) = name.replace("$","_$")

}

case class AttrId(id:Id, attributes:Seq[Attribute])

object AttrId {
  implicit def toId(aid:AttrId):Id = aid.id
}
