package samaya.structure


trait CompiledModule extends Module {
  override def functions:Seq[FunctionDef]
  override def function(index:Int):Option[FunctionDef] = Module.defaultLookup(functions,index)
  override def signatures:Seq[SignatureDef]
  override def signature(index:Int):Option[SignatureDef] = Module.defaultLookup(signatures,index)
  override def implements:Seq[ImplementDef]
  override def implement(index:Int):Option[ImplementDef] = Module.defaultLookup(implements,index)
  def substitute(dataTypes:Seq[DataDef] = dataTypes, signatures:Seq[SignatureDef] = signatures, functions:Seq[FunctionDef] = functions, implements:Seq[ImplementDef] = implements):CompiledModule
}
