package samaya.plugin.impl.compiler.mandala.inter.json

import samaya.plugin.impl.compiler.mandala.entry
import samaya.plugin.impl.compiler.mandala.components.module.{MandalaModule, MandalaModuleInterface}
import samaya.plugin.impl.compiler.mandala.inter.json.JsonModel.TypeAlias
import samaya.plugin.impl.inter.json.{DataDefImpl, FunctionSigImpl, GenericImpl, JsonLocation, JsonSource, TypeBuilder}
import samaya.structure
import samaya.structure._
import samaya.structure.types.{CompLink, Hash, InputSourceId, Region}

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

  //Todo: Not nice:
  // Improve if entry.TypeAlias becomes a proper ModuleEntry -- Sadly this currently requires position which makes no sense for non manifested ones
  override val typeAlias: Seq[entry.TypeAlias] = {
    val implLoc = location.descendProperty("typeAlias")
    input.typeAlias.map {
      case TypeAlias(name, generics, target) =>
        val loc = implLoc.descendProperty(name)
        val src = new InputSourceId(Region(loc,loc))
        val genLoc = implLoc.descendProperty("generics")
        val gens = generics.zipWithIndex.map(gi => GenericImpl(genLoc.descendProperty(gi._1.name), gi._1,gi._2))
        TypeBuilder.inContext(gens){
          val typ = TypeBuilder.toType(target, src)
          entry.TypeAlias(name,gens,typ,src)
        }
    }
  }


  override def toInterface(meta: Meta): Interface[MandalaModule] = new MandalaModuleInterface(meta, this)
  override def isVirtual: Boolean = input.link.isEmpty
}
