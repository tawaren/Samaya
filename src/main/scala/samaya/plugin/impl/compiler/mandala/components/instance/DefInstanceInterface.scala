package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.structure.types.{CompLink, ImplFunc, SourceId, StdFunc, Type}
import samaya.structure.{Generic, Interface, Meta}

class DefInstanceInterface(override val meta:Meta, private val inst: DefInstance) extends DefInstance with Interface[DefInstance] {
  override def link: CompLink = meta.link
  override def name: String = inst.name
  override def language: String = inst.language
  override def version: String = inst.version
  override def classifier: Set[String] = inst.classifier
  override def generics: Seq[Generic] = inst.generics
  override def priority: Int = inst.priority
  override def classApplies: Seq[Type] = inst.classApplies
  override def classTarget: CompLink = inst.classTarget
  override def implements: Seq[SigImplement] = inst.implements
  override def src:SourceId = inst.src

  assert(isVirtual == inst.isVirtual)
}