package samaya.plugin.impl.compiler.mandala.inter.json

import samaya.plugin.impl.compiler.mandala.components.clazz.{SigClass, SigClassInterface}
import samaya.plugin.impl.inter.json.{DataDefImpl, FunctionSigImpl, GenericImpl, JsonLocation, JsonSource}
import samaya.structure
import samaya.structure._
import samaya.structure.types.{CompLink, Hash}

class SigClassInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceSigClass) extends SigClass with JsonSource {
  override def classGenerics: Seq[Generic] = {
    val genLoc = location.descendProperty("generics")
    input.generics.zipWithIndex.map(gi => GenericImpl(genLoc.descendProperty(gi._1.name), gi._1,gi._2))
  }
  override def name: String = input.name
  override def language: String = input.language
  override def version: String = input.version
  override def classifier: Set[String] = input.classifier
  override def mode: Module.Mode = input.mode
  override val clazzLink: CompLink = CompLink.ByInterface(Hash.fromString(input.classTarget))
  override def attributes: Seq[Attribute] = Seq.empty
  override def functions: Seq[FunctionSig] = Seq.empty
  override def implements: Seq[FunctionSig] = Seq.empty

  override val dataTypes: Seq[DataDef] = {
    val funLoc = location.descendProperty("datatypes")
    input.datatypes.map(inp => DataDefImpl(funLoc.descendProperty(inp.name),inp))
  }
  override def dataType(index: Int): Option[DataDef] = dataTypes.find(f => f.index==index)

  override val signatures: Seq[structure.FunctionSig] = {
    val funLoc = location.descendProperty("signatures")
    input.signatures.map(inp => FunctionSigImpl(funLoc.descendProperty(inp.name),inp))
  }
  override def signature(index: Int): Option[structure.FunctionSig] = signatures.find(f => f.index==index)

  override def toInterface(meta: Meta): Interface[SigClass] = new SigClassInterface(meta, this)
  override def isVirtual: Boolean = true
}
