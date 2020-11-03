package samaya.plugin.impl.compiler.mandala.components.module

import samaya.plugin.impl.compiler.mandala.entry.TypeAlias
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.structure._
import samaya.structure.types.{CompLink, SourceId}

class MandalaModuleCompilerOutput(
                                   override val name:String,
                                   override val mode: Module.Mode,
                                   override val dataTypes: Seq[DataDef],
                                   override val functions: Seq[FunctionDef],
                                   override val implements: Seq[ImplementDef],
                                   override val instances:Map[CompLink, Seq[String]],
                                   override val typeAlias: Seq[TypeAlias],
                                   override val src:SourceId,
                                   override val signatures: Seq[SignatureDef] = Seq.empty
) extends MandalaModule with CompiledModule{
  override def language: String = MandalaCompiler.Language
  override def version: String = MandalaCompiler.Version
  override def classifier: Set[String] = MandalaCompiler.MandalaModule_Classifier
  override def attributes: Seq[Attribute] = Seq.empty
  override def toInterface(meta: Meta): Interface[MandalaModule] = new MandalaModuleInterface(meta, this)
  override val isVirtual: Boolean = false
}
