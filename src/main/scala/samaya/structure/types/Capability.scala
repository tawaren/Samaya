package samaya.structure.types

sealed trait Capability {
  def mask:Byte
}

object Capability {

  case object Drop extends Capability{ override def mask: Byte = 1}
  case object Copy extends Capability{ override def mask: Byte = 2}
  case object Persist extends Capability{ override def mask: Byte = 4}
  case object Primitive extends Capability{ override def mask: Byte = 8}
  case object Value extends Capability{ override def mask: Byte = 16}
  case object Unbound extends Capability{ override def mask: Byte = 32}

  val all:Set[Capability] = Set(Drop ,Copy ,Persist, Primitive, Value, Unbound)

  def fromString(str:String):Option[Capability] = {
    str.toLowerCase match {
      case "drop" => Some(Capability.Drop)
      case "copy" => Some(Capability.Copy)
      case "persist" => Some(Capability.Persist)
      case "primitive" => Some(Capability.Primitive)
      case "value" => Some(Capability.Value)
      case "unbound" => Some(Capability.Unbound)
      case _ => None
    }
  }

  def toString(cap:Capability):String = {
    cap match {
      case Drop => "drop"
      case Copy => "copy"
      case Persist => "persist"
      case Primitive => "primitive"
      case Value => "value"
      case Unbound => "unbound"
    }
  }
}