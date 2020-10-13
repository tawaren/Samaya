package samaya.structure.types

import samaya.structure.types.Capability.{Copy, Drop, Persist, Primitive, Unbound, Value}

//todo: we need origin (as Optional)
sealed trait Accessibility
object Accessibility {
  case object Global extends Accessibility
  case object Local extends Accessibility
  case class Guarded(guards:Set[String]) extends Accessibility

  def fromString(str:String, guards:Set[String]):Option[Accessibility] = {
    str.toLowerCase match {
      case "global" => Some(Accessibility.Global)
      case "local" => Some(Accessibility.Local)
      case "guarded" => Some(Accessibility.Guarded(guards))
      case _ => None
    }
  }

  def toNameString(access:Accessibility):String = {
    access match {
      case Global => "global"
      case Local => "local"
      case Guarded(_) => "guarded"
    }
  }


}
