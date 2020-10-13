package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.components.instance.Instance.EntryRef
import samaya.structure.types.{CompLink, Type}
import samaya.structure.{Interface, Meta}

class DefInstanceInterface(override val meta:Meta, private val inst: DefInstance) extends DefInstance with Interface[DefInstance] {
  override def link: CompLink = meta.link
  override def name: String = inst.name
  override def language: String = inst.language
  override def version: String = inst.version
  override def classifier: Set[String] = inst.classifier
  override def applies: Seq[Type] = inst.applies
  override def classTarget: CompLink = inst.classTarget
  override def funReferences: Map[String, EntryRef] = inst.funReferences
  override def  implReferences: Map[String, EntryRef] = inst.implReferences
  assert(isVirtual == inst.isVirtual)

}