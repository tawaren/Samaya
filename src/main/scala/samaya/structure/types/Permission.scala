package samaya.structure.types

import samaya.structure.types.Capability.{Copy, Drop, Persist, Primitive, Unbound, Value}

//todo: we need origin (as Optional)
sealed trait Permission{
  def mask:Byte
}
object Permission {
  case object Create extends Permission{ override def mask: Byte = 1}
  case object Consume extends Permission{ override def mask: Byte = 2}
  case object Inspect extends Permission{ override def mask: Byte = 4}
  case object Call extends Permission{ override def mask: Byte = 8}
  case object Define extends Permission{ override def mask: Byte = 16}

  def fromString(str:String):Option[Permission] = {
    str.toLowerCase match {
      case "create" => Some(Permission.Create)
      case "consume" => Some(Permission.Consume)
      case "inspect" => Some(Permission.Inspect)
      case "call" => Some(Permission.Call)
      case "define" => Some(Permission.Define)
      case _ => None
    }
  }

  def toString(perm:Permission):String = {
    perm match {
      case Create => "create"
      case Consume => "consume"
      case Inspect => "inspect"
      case Call => "call"
      case Define => "define"
    }
  }

}

