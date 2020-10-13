package samaya.plugin.impl.compiler.mandala.components.module

import samaya.structure.Module.Mode
import samaya.structure._
import samaya.structure.types.CompLink

class MandalaModuleInterface(override val meta:Meta, private val module: MandalaModule) extends MandalaModule with Interface[MandalaModule] {
  override def link: CompLink = meta.link
  override def name: String = module.name
  override def language: String = module.language
  override def version: String = module.version
  override def classifier: Set[String] = module.classifier
  override def mode: Mode = module.mode
  override def attributes: Seq[Attribute] = module.attributes
  override def functions: Seq[FunctionSig] = module.functions
  override def signatures: Seq[FunctionSig] = module.signatures
  override def implements: Seq[FunctionSig] = module.implements
  override def dataTypes: Seq[DataDef] = module.dataTypes
  override def instances: Map[CompLink,Seq[String]] = module.instances
  override def toInterface(meta: Meta): Interface[MandalaModule] = new MandalaModuleInterface(meta,module)
  override def isVirtual: Boolean = module.isVirtual
}