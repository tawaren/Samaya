package samaya.plugin.impl.compiler.mandala.components

import samaya.plugin.impl.compiler.mandala.components.instance.Instance.EntryRef
import samaya.structure.types.{CompLink, Type}

trait InstInfo {
  def name:String
  def classTarget: CompLink
  def applies: Seq[Type]
  def funReferences: Map[String, EntryRef]
  def implReferences: Map[String, EntryRef]
}
