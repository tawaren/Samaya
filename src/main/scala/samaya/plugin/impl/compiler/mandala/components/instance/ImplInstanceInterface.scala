package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.structure.types.{CompLink, SourceId, Type}
import samaya.structure.{FunctionSig, Generic, Interface, Meta, Module}

class ImplInstanceInterface(override val meta:Meta, private val inst: ImplInstance) extends ImplInstance with Interface[ImplInstance] {
  override def link: CompLink = meta.link
  override def name: String = inst.name
  override def mode: Module.Mode = inst.mode
  override def language: String = inst.language
  override def version: String = inst.version
  override def classifier: Set[String] = inst.classifier
  override def generics: Seq[Generic] = inst.generics
  override def classApplies: Seq[Type] = inst.classApplies
  override def classTarget: CompLink = inst.classTarget
  override def implements: Seq[FunctionSig] = inst.implements
  override def src:SourceId = inst.src
  assert(isVirtual == inst.isVirtual)

}