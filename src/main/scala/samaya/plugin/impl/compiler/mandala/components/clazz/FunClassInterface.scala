package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure._
import samaya.structure.types.CompLink

class FunClassInterface(override val meta:Meta, private val cls: FunClass) extends FunClass with Interface[FunClass] {
  override def link: CompLink = meta.link
  override def name: String = cls.name
  override def language: String = cls.language
  override def version: String = cls.version
  override def classifier: Set[String] = cls.classifier
  override def classGenerics: Seq[Generic] = cls.classGenerics
  override def mode: Module.Mode = cls.mode
  override def attributes: Seq[Attribute] = cls.attributes
  override def dataTypes: Seq[DataDef] = cls.dataTypes
  override def signatures: Seq[FunctionSig] = cls.signatures
  override def functions: Seq[FunctionSig] = cls.functions
  override def implements: Seq[FunctionSig] = cls.implements
  override def toInterface(meta: Meta): Interface[FunClass] = new FunClassInterface(meta, cls)
  override def isVirtual: Boolean = cls.isVirtual

}