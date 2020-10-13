package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.structure.{CompiledModule, DataDef, FunctionDef, Module, ImplementDef, Interface, Meta, SignatureDef}
import samaya.structure.types.{CompLink, Type}

class MandalaImplInstanceCompilerOutput(
                                         override val name:String,
                                         override val mode: Module.Mode,
                                         override val classTarget: CompLink,
                                         override val applies: Seq[Type],
                                         override val implements: Seq[ImplementDef]
) extends ImplInstance with CompiledModule {
  override def signatures: Seq[SignatureDef] = Seq.empty
  override def functions: Seq[FunctionDef] = Seq.empty
  override def toInterface(meta: Meta): Interface[ImplInstance] = new ImplInstanceInterface(meta, this)
  override def substitute(dataTypes: Seq[DataDef], signatures: Seq[SignatureDef], functions: Seq[FunctionDef], implements: Seq[ImplementDef]): CompiledModule = {
    assert(dataTypes.isEmpty)
    assert(functions.isEmpty)
    assert(signatures.isEmpty)
    new MandalaImplInstanceCompilerOutput(name,mode,classTarget,applies,implements)
  }
}