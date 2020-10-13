package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure._

class MandalaFunClassCompilerOutput(
   override val name:String,
   override val mode: Module.Mode,
   override val classGenerics: Seq[Generic],
   override val functions: Seq[FunctionSig],
) extends FunClass {
  override val isVirtual: Boolean = true
  override def toInterface(meta: Meta): Interface[FunClass] = new FunClassInterface(meta, this)
}
