package samaya.types

import samaya.structure.types.Hash

sealed trait Address

object Address {

  sealed trait LocationBased extends Address {def elements: Seq[Identifier]}
  case class Relative(override val elements:Seq[Identifier]) extends LocationBased{
    override def toString: String = elements.mkString("/")
  }
  case class Absolute(protocol:String, override val elements:Seq[Identifier]) extends LocationBased{
    override def toString: String = protocol+"://"+elements.mkString("/")
  }
  case class ContentBased(target:Hash) extends Address {
    override def toString: String = "content://"+target
  }
  case class HybridAddress(target:ContentBased, loc:LocationBased) extends Address{
    override def toString: String = target+"@"+loc
  }

  def apply(): Relative = Relative(Seq.empty)
  def apply(name:String): Relative = Relative(Seq(Identifier(name)))
  def apply(name:String, extension:String): Relative = Relative(Seq(Identifier(name, extension)))
  def apply(ident:Identifier): Relative = Relative(Seq(ident))
  def apply(hash:Hash): ContentBased = ContentBased(hash)

}