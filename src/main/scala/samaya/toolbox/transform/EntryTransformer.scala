package samaya.toolbox.transform

import samaya.structure.types.{OpCode, SourceId}
import samaya.structure.{Attribute, CompiledTransaction, DataDef, FunctionDef, ImplementDef, Package, Param, Result, SignatureDef}
import samaya.types.Context

trait EntryTransformer {
  self =>

  def andThen(next:EntryTransformer):EntryTransformer = new EntryTransformer {
    override def transformFunction(in: FunctionDef, context: Context): FunctionDef = next.transformFunction(self.transformFunction(in, context), context)
    override def transformImplement(in: ImplementDef, context: Context): ImplementDef = next.transformImplement(self.transformImplement(in, context), context)
    override def transformSignature(in: SignatureDef, context: Context): SignatureDef = next.transformSignature(self.transformSignature(in, context), context)
    override def transformDataType(in: DataDef, context: Context): DataDef = next.transformDataType(self.transformDataType(in, context), context)
    override def transformTransaction(in: CompiledTransaction, pkg: Package): CompiledTransaction = next.transformTransaction(self.transformTransaction(in, pkg), pkg)
  }

  def transformFunction(in:FunctionDef, context:Context):FunctionDef = in
  def transformImplement(in:ImplementDef, context:Context):ImplementDef = in
  def transformSignature(in:SignatureDef, context:Context):SignatureDef = in
  def transformDataType(in:DataDef, context:Context):DataDef = in

  def transformTransaction(in: CompiledTransaction, pkg: Package): CompiledTransaction = {
    val ctx = Context(pkg)
    val adapted = transformFunction(in, ctx)
    new CompiledTransaction {
      override val name: String = in.name
      override val language: String = in.language
      override val version: String = in.version
      override val classifier: Set[String] = in.classifier
      override val attributes: Seq[Attribute] = in.attributes
      override val transactional: Boolean = adapted.transactional
      override val params: Seq[Param] = adapted.params
      override val results: Seq[Result] = adapted.results
      override val code: Seq[OpCode] = adapted.code
      override val src:SourceId = adapted.src
      override val isVirtual: Boolean = in.isVirtual
    }
  }
}