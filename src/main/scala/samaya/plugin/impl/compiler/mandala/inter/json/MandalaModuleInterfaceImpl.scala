package samaya.plugin.impl.compiler.mandala.inter.json

import samaya.plugin.impl.compiler.mandala.components.module.{MandalaModule, MandalaModuleInterface}
import samaya.plugin.impl.inter.json.{DataDefImpl, FunctionSigImpl, JsonLocation, JsonSource}
import samaya.structure
import samaya.structure._
import samaya.structure.types.{CompLink, Hash}

class MandalaModuleInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceMandalaModule) extends MandalaModule with JsonSource {
  override def name: String = input.name
  override def language: String = input.language
  override def version: String = input.version
  override def classifier: Set[String] = input.classifier
  override def mode: Module.Mode = input.mode
  override def attributes: Seq[Attribute] = Seq.empty
  override def signatures: Seq[FunctionSig] = Seq.empty

  override val functions: Seq[structure.FunctionSig] = {
    val funLoc = location.descendProperty("functions")
    input.functions.map(inp => FunctionSigImpl(funLoc.descendProperty(inp.name),inp))
  }
  override def function(index: Int): Option[structure.FunctionSig] = functions.find(f => f.index==index)

  override val implements: Seq[structure.FunctionSig] = {
    val implLoc = location.descendProperty("implements")
    input.implements.map(inp => FunctionSigImpl(implLoc.descendProperty(inp.name),inp))
  }
  override def implement(index: Int): Option[structure.FunctionSig] = implements.find(f => f.index==index)


  override val dataTypes: Seq[DataDef] = {
    val dataLoc = location.descendProperty("dataTypes")
    input.datatypes.map(inp => DataDefImpl(dataLoc.descendProperty(inp.name),inp))
  }
  override def dataType(index: Int): Option[structure.DataDef] = dataTypes.find(f => f.index==index)

  override val instances: Map[CompLink, Seq[String]] = input.instances.map{
    case JsonModel.InstanceEntry(clazz, instances) => (CompLink.ByInterface(Hash.fromString(clazz)), instances)
  }.toMap

  override def toInterface(meta: Meta): Interface[MandalaModule] = new MandalaModuleInterface(meta, this)
  override def isVirtual: Boolean = true
}
