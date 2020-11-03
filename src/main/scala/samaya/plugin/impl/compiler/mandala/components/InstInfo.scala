package samaya.plugin.impl.compiler.mandala.components

import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.structure.Generic
import samaya.structure.types.{CompLink, Type}

trait InstInfo {
  def name:String
  def generics: Seq[Generic]
  def classTarget: CompLink
  def classApplies: Seq[Type]
  def implements: Seq[SigImplement]
}
