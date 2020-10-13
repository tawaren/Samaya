package samaya.plugin.impl.compiler.mandala.components.clazz

import samaya.structure._
import samaya.structure.types.CompLink

class MandalaSigClassCompilerOutput(
   override val name:String,
   override val mode: Module.Mode,
   override val clazzLink: CompLink,
   override val classGenerics: Seq[Generic],
   override val dataTypes: Seq[DataDef],
   override val signatures: Seq[SignatureDef],
) extends SigClass with CompiledModule {
  override val isVirtual: Boolean = false
  override val functions: Seq[FunctionDef] = Seq.empty
  override val implements: Seq[ImplementDef] = Seq.empty
  override def toInterface(meta: Meta): Interface[SigClass] = new SigClassInterface(meta, this)
  override def substitute(dataTypes: Seq[DataDef], signatures: Seq[SignatureDef], functions: Seq[FunctionDef], implements: Seq[ImplementDef]): CompiledModule = {
    assert(functions.isEmpty)
    assert(implements.isEmpty)
    new MandalaSigClassCompilerOutput(name,mode,clazzLink,classGenerics,dataTypes,signatures)
  }
}
