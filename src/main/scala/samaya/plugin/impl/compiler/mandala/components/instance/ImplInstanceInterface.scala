package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.structure.types.{CompLink, Type}
import samaya.structure.{FunctionSig, Interface, Meta, Module}

class ImplInstanceInterface(override val meta:Meta, private val inst: ImplInstance) extends ImplInstance with Interface[ImplInstance] {
  override def link: CompLink = meta.link
  override def name: String = inst.name
  override def mode: Module.Mode = inst.mode
  override def language: String = inst.language
  override def version: String = inst.version
  override def classifier: Set[String] = inst.classifier
  override def applies: Seq[Type] = inst.applies
  override def classTarget: CompLink = inst.classTarget
  override def implements: Seq[FunctionSig] = inst.implements
  assert(isVirtual == inst.isVirtual)

}