package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure._
import samaya.structure.types.SourceId

class MandalaFunClassCompilerOutput(
                                     override val name:String,
                                     override val mode: Module.Mode,
                                     override val generics: Seq[Generic],
                                     override val functions: Seq[FunctionSig],
                                     override val src:SourceId
) extends FunClass {
  override val isVirtual: Boolean = true
  override def toInterface(meta: Meta): Interface[FunClass] = {
    new FunClassInterface(meta, this)
  }
}
