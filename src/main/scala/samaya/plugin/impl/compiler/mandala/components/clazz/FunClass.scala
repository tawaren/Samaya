package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.structure.{DataDef, FunctionSig, Interface, Meta}

trait FunClass extends Class {
  override def language: String = MandalaCompiler.Language
  override def version: String = MandalaCompiler.Version
  override def classifier: Set[String] = MandalaCompiler.FunClass_Classifier
  override def signatures: Seq[FunctionSig] = Seq.empty
  override def dataTypes: Seq[DataDef] = Seq.empty
  override def toInterface(meta: Meta): Interface[FunClass]
}
