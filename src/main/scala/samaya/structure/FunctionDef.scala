package samaya.structure

import samaya.structure.types.{Capability, OpCode}
trait FunctionDef extends FunctionSig with ModuleEntry {
  def code:Seq[OpCode]
  def external:Boolean

  override def capabilities:Set[Capability] = Set.empty
}
