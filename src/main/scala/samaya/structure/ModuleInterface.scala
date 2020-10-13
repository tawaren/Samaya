package samaya.structure

import samaya.structure.types.CompLink

class ModuleInterface(override val meta:Meta, private val module: Module) extends Module with Interface[Module] {
  override def toInterface(meta: Meta): Interface[Module] = new ModuleInterface(meta,module)
  override def link: CompLink = meta.link
  override def name: String = module.name
  override def language: String = module.language
  override def version: String = module.version
  override def classifier: Set[String] = module.classifier
  override def mode: Module.Mode = module.mode
  override def attributes: Seq[Attribute] = module.attributes
  override def functions: Seq[FunctionSig] = module.functions
  override def signatures: Seq[FunctionSig] = module.signatures
  override def implements: Seq[FunctionSig] = module.implements
  override def dataTypes: Seq[DataDef] = module.dataTypes
  override def isVirtual: Boolean = module.isVirtual
}
