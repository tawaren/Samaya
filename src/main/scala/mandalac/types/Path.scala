package mandalac.types

sealed trait Path {
  def elements:Seq[Identifier]
}

object Path {
  //protocoll is defined by parent
  case class Relative(override val elements:Seq[Identifier]) extends Path
  case class Absolute(protocol:String, override val elements:Seq[Identifier]) extends Path

  def apply(name:String): Relative = Relative(List(Identifier(name)))
  def apply(name:String, extension:String): Relative = Relative(List(Identifier(name, extension)))
  def apply(ident:Identifier): Relative = Relative(List(ident))

}