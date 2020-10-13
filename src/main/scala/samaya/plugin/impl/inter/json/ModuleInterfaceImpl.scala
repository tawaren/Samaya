package samaya.plugin.impl.inter.json

import samaya.structure
import samaya.structure.{Attribute, DataDef, Interface, Meta, Module, ModuleInterface}

class ModuleInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceModule) extends Module with JsonSource {
  override val name: String = input.name
  override val language: String = input.language
  override val version: String = input.version
  override val classifier: Set[String] = input.classifier
  override val attributes: Seq[Attribute] = input.attributes
  override val mode: Module.Mode = input.mode //we never need to deploy them so this is io
  override val isVirtual: Boolean = input.link.isEmpty

  override val functions: Seq[structure.FunctionSig] = {
    val funLoc = location.descendProperty("functions")
    input.functions.map(inp => FunctionSigImpl(funLoc.descendProperty(inp.name),inp))
  }
  override def function(index: Int): Option[structure.FunctionSig] = functions.find(f => f.index==index)
  override val dataTypes: Seq[DataDef] = {
    val dataLoc = location.descendProperty("dataTypes")
    input.datatypes.map(inp => DataDefImpl(dataLoc.descendProperty(inp.name),inp))
  }
  override def dataType(index: Int):Option[DataDef] = dataTypes.find(f => f.index==index)
  override val signatures: Seq[structure.FunctionSig] = {
    val sigLoc = location.descendProperty("signatureTypes")
    input.sigtypes.map(inp => FunctionSigImpl(sigLoc.descendProperty(inp.name),inp))
  }
  override def signature(index: Int): Option[structure.FunctionSig] = signatures.find(f => f.index==index)

  override val implements: Seq[structure.FunctionSig] = {
    val implLoc = location.descendProperty("implements")
    input.implements.map(inp => FunctionSigImpl(implLoc.descendProperty(inp.name),inp))
  }
  override def implement(index: Int): Option[structure.FunctionSig] = implements.find(f => f.index==index)
  override def toInterface(meta: Meta): Interface[Module] = new ModuleInterface(meta, this)

}
