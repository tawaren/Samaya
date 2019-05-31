package mandalac.plugin.impl.compiler.dummy
import mandalac.structure.meta.ModuleAttribute
import mandalac.structure.{DataType, FunctionDef, ModuleEssentials, Risk}

class DummyCompilerOutput(override val name:String) extends ModuleEssentials{
  override def language: String = DummyCompiler.Language
  override def version: String = DummyCompiler.Version
  override def classifier: String = DummyCompiler.classifiers.head
  override def attributes: Seq[ModuleAttribute] = Seq.empty
  override def functions: Seq[FunctionDef] = Seq.empty
  override def dataTypes: Seq[DataType] = Seq.empty
  override def risks: Seq[Risk] = Seq.empty
}
