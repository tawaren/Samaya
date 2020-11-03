package samaya.structure

case class Attribute(name:String, value:Attribute.Value)

object Attribute {
  sealed trait Value
  case object Unit extends Value
  case class Number(num:BigInt) extends Value
  case class Text(text:String) extends Value
  case class Flag(flag:Boolean) extends Value
  case class Struct(struct:Map[String, Attribute.Value]) extends Value
  case class Array(arr:Seq[Attribute.Value]) extends Value
}