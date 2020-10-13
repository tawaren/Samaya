package samaya.structure.types

import samaya.structure.Attribute

class Id private (override val name:String, override val src:Option[Region]) extends Ref {

  override def id: Id = this

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

  def apply():Id = new Id("$#"+nextNo(),None)
  def apply(num:Int):Id = new Id("$"+num,None)
  def apply(num:Int, reg:Region):Id = new Id("$"+num, Some(reg))

  def apply(name:String):Id = new Id(name.replaceAllLiterally("$","$$"),None)
  def apply(name:String, reg:Region):Id = new Id(name.replaceAllLiterally("$","$$"), Some(reg))
  def apply(id:Id):Id = new Id(s"${id.name}#${nextNo()}", id.src)

}

case class AttrId(id:Id, attributes:Seq[Attribute])

object AttrId {
  implicit def toId(aid:AttrId):Id = aid.id
}