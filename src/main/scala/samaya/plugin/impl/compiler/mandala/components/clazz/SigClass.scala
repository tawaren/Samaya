package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.structure.types.CompLink
import samaya.structure.{FunctionSig, Interface, Meta}

trait SigClass extends Class {
  def clazzLink:CompLink
  override def language: String = MandalaCompiler.Language
  override def version: String = MandalaCompiler.Version
  override def classifier: Set[String] = MandalaCompiler.SigClass_Classifier
  override def functions: Seq[FunctionSig] = Seq.empty
  override def toInterface(meta: Meta): Interface[SigClass]
}
