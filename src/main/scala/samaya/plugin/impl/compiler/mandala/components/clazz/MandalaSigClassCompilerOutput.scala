package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure._
import samaya.structure.types.{CompLink, SourceId}

class MandalaSigClassCompilerOutput(
                                     override val name:String,
                                     override val mode: Module.Mode,
                                     override val clazzLink: CompLink,
                                     override val generics: Seq[Generic],
                                     override val signatures: Seq[SignatureDef],
                                     override val src:SourceId
) extends SigClass with CompiledModule {
  override val isVirtual: Boolean = false
  override val dataTypes: Seq[DataDef] = Seq.empty
  override val functions: Seq[FunctionDef] = Seq.empty
  override val implements: Seq[ImplementDef] = Seq.empty
  override def toInterface(meta: Meta): Interface[SigClass] = new SigClassInterface(meta, this)
}
