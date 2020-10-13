package samaya.structure

import samaya.structure.types.{Capability, OpCode, SigType, Type}

//The sig is: Capture -> DataType (Signature)
trait ImplementDef extends FunctionSig with ModuleEntry {
  //Names given to the sig values
  def sigParamBindings:Seq[Binding]
  def sigResultBindings:Seq[Binding]
  lazy val implements:SigType = {
    assert(results.size == 1)
    result(0).get.typ.asSigType.get
  }
  def code:Seq[OpCode]
  def external:Boolean
  override def capabilities:Set[Capability] = Set.empty
}
