package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.structure.{Attribute, DataDef, FunctionSig, Interface, Meta, Module}

trait ImplInstance extends Instance with Module {
  override def language: String = MandalaCompiler.Language
  override def version: String = MandalaCompiler.Version
  override def classifier: Set[String] = MandalaCompiler.ImplInstance_Classifier
  override def attributes: Seq[Attribute] = Seq.empty
  override def dataTypes: Seq[DataDef] = Seq.empty
  override def signatures: Seq[FunctionSig] = Seq.empty
  override def functions: Seq[FunctionSig] = Seq.empty
  override def isVirtual: Boolean = false
  override def toInterface(meta: Meta): Interface[ImplInstance] = new ImplInstanceInterface(meta, this)

}
