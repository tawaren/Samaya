package mandalac.structure.types

sealed trait Capability {
  def recursive: Boolean
  def mask:Byte
}

object Capability {

  case object Drop extends Capability{ override def mask: Byte = 1; override def recursive: Boolean = true }
  case object Copy extends Capability{ override def mask: Byte = 2; override def recursive: Boolean = true }
  case object Persist extends Capability{ override def mask: Byte = 4; override def recursive: Boolean = true }
  case object Consume extends Capability{ override def mask: Byte = 8; override def recursive: Boolean = false }
  case object Inspect extends Capability{ override def mask: Byte = 16; override def recursive: Boolean = false }
  case object Embed extends Capability{ override def mask: Byte = 32; override def recursive: Boolean = false }
  case object Create extends Capability{ override def mask: Byte = 64; override def recursive: Boolean = false }

  val recursives:Set[Capability] = Set(Drop ,Copy ,Persist)
  val all:Set[Capability] = Set(Drop ,Copy ,Persist, Consume, Inspect, Embed, Create)

  def fromString(str:String):Option[Capability] = {
    str.toLowerCase match {
      case "drop" => Some(Capability.Drop)
      case "copy" => Some(Capability.Copy)
      case "persist" => Some(Capability.Persist)
      case "consume" => Some(Capability.Consume)
      case "inspect" => Some(Capability.Inspect)
      case "embed" => Some(Capability.Embed)
      case "create" => Some(Capability.Create)
      case _ => None
    }
  }

  def toString(cap:Capability):String = {
    cap match {
      case Drop => "drop"
      case Copy => "copy"
      case Persist => "persist"
      case Consume => "consume"
      case Inspect => "inspect"
      case Embed => "embed"
      case Create => "create"
    }
  }
}