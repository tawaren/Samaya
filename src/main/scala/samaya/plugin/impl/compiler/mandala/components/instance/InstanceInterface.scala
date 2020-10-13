package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.structure.types.{CompLink, Type}
import samaya.structure.{Interface, Meta}

class InstanceInterface(override val meta:Meta, private val inst: Instance) extends Instance with Interface[Instance] {
  override def link: CompLink = meta.link
  override def name: String = inst.name
  override def language: String = inst.language
  override def version: String = inst.version
  override def classifier: Set[String] = inst.classifier
  override def applies: Seq[Type] = inst.applies
  override def classTarget: CompLink = inst.classTarget
  override def toInterface(meta: Meta): Interface[Instance] = new InstanceInterface(meta,inst)
  override def isVirtual: Boolean = inst.isVirtual
}