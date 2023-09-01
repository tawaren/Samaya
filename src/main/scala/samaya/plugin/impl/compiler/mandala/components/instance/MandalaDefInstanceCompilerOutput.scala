package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.structure.{Generic, Interface, Meta}
import samaya.structure.types.{CompLink, ImplFunc, SourceId, StdFunc, Type}

class MandalaDefInstanceCompilerOutput(
                                        override val name:String,
                                        override val priority: Int,
                                        override val generics: Seq[Generic],
                                        override val classTarget: CompLink,
                                        override val classApplies: Seq[Type],
                                        override val implements: Seq[SigImplement],
                                        override val src:SourceId
) extends DefInstance {
  override def toInterface(meta: Meta): Interface[DefInstance] = new DefInstanceInterface(meta, this)
}