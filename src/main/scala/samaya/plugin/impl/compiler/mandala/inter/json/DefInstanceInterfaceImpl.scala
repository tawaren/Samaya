package samaya.plugin.impl.compiler.mandala.inter.json

import JsonModel.{ Applied, Implement}
import samaya.plugin.impl.compiler.mandala.components.instance.DefInstance
import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.plugin.impl.inter.json.{GenericImpl, JsonLocation, JsonSource, TypeBuilder}
import samaya.structure.Generic
import samaya.structure.types.{CompLink,  Hash, ImplFunc, InputSourceId, Region, StdFunc, Type}


class DefInstanceInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceInstance) extends DefInstance with JsonSource {
  override val name: String = input.name
  override val language: String = input.language
  override val version: String = input.version
  override val classifier: Set[String] = input.classifier
  override def priority: Int = input.priority
  override val generics: Seq[Generic] = {
    val genLoc = location.descendProperty("generics")
    input.generics.zipWithIndex.map(gi => GenericImpl(genLoc.descendProperty(gi._1.name), gi._1,gi._2))
  }

  override val classTarget: CompLink = CompLink.ByInterface(Hash.fromString(input.classTarget))
  override val classApplies: Seq[Type] = TypeBuilder.inContext(generics){
    input.applies.map(TypeBuilder.toType(_, location.descendProperty("class_applies")))
  }

  //Todo: Not nice:
  // Improve if SigImplement becomes a proper ModuleEntry -- Sadly this currently requires position which makes no sense for non manifested ones
  override val implements: Seq[SigImplement] = {
    val implLoc = location.descendProperty("implements")
    input.implements.map{
      case Implement(name,impl_generics,fun,impl) =>
        val loc = implLoc.descendProperty(name)
        val src = new InputSourceId(Region(loc,loc))
        val genLoc = implLoc.descendProperty("generics")
        val gens = impl_generics.zipWithIndex.map(gi => GenericImpl(genLoc.descendProperty(gi._1.name), gi._1,gi._2))
        TypeBuilder.inContext(gens) {
          val nFun = fun match {
              case Applied(Some(module), entryIndex, applies) => StdFunc.Remote(CompLink.fromString(module), entryIndex, applies.map(TypeBuilder.toType(_, src)))(src)
              case Applied(None, entryIndex, applies) => StdFunc.Local(entryIndex, applies.map(TypeBuilder.toType(_, src)))(src)
          }
          val nImpl = impl match {
              case Applied(Some(module), entryIndex, applies) => ImplFunc.Remote(CompLink.fromString(module), entryIndex, applies.map(TypeBuilder.toType(_,src)))(src)
              case Applied(None, entryIndex, applies) => ImplFunc.Local(entryIndex, applies.map(TypeBuilder.toType(_,src)))(src)
          }
          SigImplement(name,gens,nFun,nImpl,src)
        }
    }
  }

  override val isVirtual: Boolean = true
}
