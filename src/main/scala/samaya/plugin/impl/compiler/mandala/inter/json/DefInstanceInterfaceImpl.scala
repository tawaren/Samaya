package samaya.plugin.impl.compiler.mandala.inter.json

import JsonModel.{Alias, EntryRef}
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.impl.inter.json.{JsonLocation, JsonSource, TypeBuilder}
import samaya.structure.types.{CompLink, Hash, Type}


class DefInstanceInterfaceImpl(override val location: JsonLocation, input:JsonModel.InterfaceInstance) extends DefInstance with JsonSource {
  override val name: String = input.name
  override val language: String = input.language
  override val version: String = input.version
  override val classifier: Set[String] = input.classifier
  override def classTarget: CompLink = CompLink.ByInterface(Hash.fromString(input.classTarget))
  override def applies: Seq[Type] = input.applies.map(TypeBuilder.toType)

  //locals are for keeping it safe in case of future extensions they should never be serialized in an DefInstance
  override def funReferences: Map[String, Instance.EntryRef] = input.funAliases.map {
    case Alias(name, EntryRef(module, componentIndex)) =>(name, Instance.RemoteEntryRef(CompLink.fromString(module), componentIndex))
  }.toMap

  override def implReferences: Map[String, Instance.EntryRef] = input.funAliases.map {
    case Alias(name, EntryRef(module, componentIndex)) =>(name, Instance.RemoteEntryRef(CompLink.fromString(module), componentIndex))
  }.toMap

  override val isVirtual: Boolean = true
}
