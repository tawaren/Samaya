package samaya.toolbox.transform

import samaya.compilation.ErrorManager.producesErrorValue
import samaya.structure.types.{OpCode, SourceId}
import samaya.structure.{Attribute, CompiledModule, CompiledTransaction, Component, DataDef, FunctionDef, ImplementDef, Package, Param, Result, SignatureDef, Transaction}
import samaya.types.Context

trait ComponentTransformer {

  def transformComponent(cmp:Component,  pkg: Package): Option[Component] = {
    cmp match {
      case module: CompiledModule => transformModule(module, pkg)
      case transaction: CompiledTransaction => transformTransaction(transaction, pkg)
      case other => Some(other)
    }
  }

  private def transformTransaction(in: CompiledTransaction, pkg: Package): Option[Transaction] = producesErrorValue {
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
      override val src: SourceId = adapted.src
      override val isVirtual: Boolean = in.isVirtual

    }
  }


  private def transformModule(in:CompiledModule, pkg:Package):Option[CompiledModule] = producesErrorValue {
    val ctx = Context(in,pkg)
    in.substitute(
      functions = in.functions.map(transformFunction(_,ctx)),
      implements = in.implements.map(transformImplement(_,ctx)),
      signatures = in.signatures.map(transformSignature(_,ctx))
    )
  }

  def transformFunction(in:FunctionDef, context:Context):FunctionDef = in
  def transformImplement(in:ImplementDef, context:Context):ImplementDef = in
  def transformSignature(in:SignatureDef, context:Context):SignatureDef = in
  def transformDataType(in:DataDef, context:Context):DataDef = in

}
