package samaya.plugin.impl.compiler.mandala.inter.json

import samaya.plugin.impl.compiler.mandala.components.clazz.{FunClass, FunClassInterface}
import samaya.plugin.impl.inter.json.{FunctionSigImpl, GenericImpl, JsonLocation, JsonSource}
import samaya.structure
import samaya.structure.{Attribute, Module, DataDef, FunctionSig, Generic, Interface, Meta}

class FunClassInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceFunClass) extends FunClass with JsonSource {
  override def generics: Seq[Generic] = {
    val genLoc = location.descendProperty("generics")
    input.generics.zipWithIndex.map(gi => GenericImpl(genLoc.descendProperty(gi._1.name), gi._1,gi._2))
  }
  override def name: String = input.name
  override def language: String = input.language
  override def version: String = input.version
  override def classifier: Set[String] = input.classifier
  override def mode:Module.Mode = Module.Normal
  override def attributes: Seq[Attribute] = Seq.empty
  override def dataTypes: Seq[DataDef] = Seq.empty
  override def signatures: Seq[FunctionSig] = Seq.empty
  override def implements: Seq[FunctionSig] = Seq.empty

  override val functions: Seq[structure.FunctionSig] = {
    val funLoc = location.descendProperty("functions")
    input.functions.map(inp => FunctionSigImpl(funLoc.descendProperty(inp.name),inp))
  }
  override def function(index: Int): Option[structure.FunctionSig] = functions.find(f => f.index==index)

  override def toInterface(meta: Meta): Interface[FunClass] = new FunClassInterface(meta, this)
  override def isVirtual: Boolean = true
}
