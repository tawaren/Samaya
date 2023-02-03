package samaya.types

import samaya.structure.types.Hash

sealed trait Address

object Address {

  sealed trait LocationBased extends Address {def elements: Seq[Identifier]}
  case class Relative(override val elements:Seq[Identifier]) extends LocationBased
  case class Absolute(protocol:String, override val elements:Seq[Identifier]) extends LocationBased
  case class ContentBased(target:Hash) extends Address
  case class HybridAddress(target:ContentBased, loc:LocationBased) extends Address

  def apply(name:String): Relative = Relative(List(Identifier(name)))
  def apply(name:String, extension:String): Relative = Relative(List(Identifier(name, extension)))
  def apply(ident:Identifier): Relative = Relative(List(ident))
  def apply(hash:Hash): ContentBased = ContentBased(hash)

}