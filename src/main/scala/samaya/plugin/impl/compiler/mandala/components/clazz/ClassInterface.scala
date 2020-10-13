package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure.types.CompLink
import samaya.structure._

class ClassInterface(override val meta:Meta, private val cls: Class) extends Class with Interface[Class] {
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
  override def toInterface(meta: Meta): Interface[Class] = new ClassInterface(meta, this)
  override def isVirtual: Boolean = cls.isVirtual
}