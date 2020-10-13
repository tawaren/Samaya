package samaya.plugin.impl.compiler.mandala.components

import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.structure._
import samaya.structure.types.{OpCode, SourceId}

class MandalaTransactionCompilerOutput(
                            override val name:String,
                            override val transactional: Boolean,
                            override val params: Seq[Param],
                            override val results: Seq[Result],
                            override val code: Seq[OpCode],
                            override val src: SourceId
) extends CompiledTransaction {
  override def language: String = MandalaCompiler.Language
  override def version: String = MandalaCompiler.Version
  override def classifier: Set[String] = MandalaCompiler.Transaction_Classifier
  override def attributes: Seq[Attribute] = Seq.empty
  override val isVirtual: Boolean = false
}
