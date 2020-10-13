package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure.{Attribute, DataDef, FunctionSig, Generic, Interface, Meta, Module}

trait Class extends Module {
  def classGenerics: Seq[Generic]
  override def attributes: Seq[Attribute] = Seq.empty
  override def implements: Seq[FunctionSig] = Seq.empty
  def toInterface(meta: Meta): Interface[Class]
}
