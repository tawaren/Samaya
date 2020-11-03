package samaya.structure

import samaya.structure.types.{Accessibility, OpCode, Permission, SourceId}

trait CompiledTransaction extends Transaction {
  def code: Seq[OpCode]
}


object CompiledTransaction {
  implicit class TransactionFunctionDef(val txt:CompiledTransaction) extends FunctionDef {
    override def index: Int = 0
    override def position: Int = 0
    override def accessibility: Map[Permission, Accessibility] = Map(Permission.Call -> Accessibility.Global)
    override def generics: Seq[Generic] = Seq.empty
    override def attributes: Seq[Attribute] = txt.attributes
    override def code: Seq[OpCode] = txt.code
    override def external: Boolean = false
    override def name: String = txt.name
    override def transactional: Boolean = txt.transactional
    override def params: Seq[Param] = txt.params
    override def results: Seq[Result] = txt.results
    override def src:SourceId = txt.src


  }
}
