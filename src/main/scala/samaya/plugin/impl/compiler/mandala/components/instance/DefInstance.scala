package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.EntryRef
import samaya.structure.{Interface, Meta}

trait DefInstance extends Instance with InstInfo {
  override def language: String = MandalaCompiler.Language
  override def version: String = MandalaCompiler.Version
  override def classifier: Set[String] = MandalaCompiler.DefInstance_Classifier
  def funReferences: Map[String, EntryRef]
  def implReferences: Map[String, EntryRef]
  override def isVirtual: Boolean = true
  override def toInterface(meta: Meta): Interface[DefInstance] = new DefInstanceInterface(meta, this)
}
