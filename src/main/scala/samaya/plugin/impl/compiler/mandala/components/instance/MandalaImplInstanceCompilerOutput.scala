package samaya.plugin.impl.compiler.mandala.components.instance

import samaya.structure.{CompiledModule, DataDef, FunctionDef, Generic, ImplementDef, Interface, Meta, Module, SignatureDef}
import samaya.structure.types.{CompLink, SourceId, Type}

class MandalaImplInstanceCompilerOutput(
                                         override val name:String,
                                         override val mode: Module.Mode,
                                         override val generics: Seq[Generic],
                                         override val classTarget: CompLink,
                                         override val classApplies: Seq[Type],
                                         override val implements: Seq[ImplementDef],
                                         override val src:SourceId
) extends ImplInstance with CompiledModule {
  override def signatures: Seq[SignatureDef] = Seq.empty
  override def functions: Seq[FunctionDef] = Seq.empty
  override def toInterface(meta: Meta): Interface[ImplInstance] = new ImplInstanceInterface(meta, this)
}