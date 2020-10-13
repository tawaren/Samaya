package samaya.plugin.impl.inter.json

import samaya.structure.types.Capability
import samaya.structure.{Attribute, Generic}

case class GenericImpl(override val location: JsonLocation, generic: JsonModel.Generic, override val index:Int) extends Generic with JsonSource {
  override val name: String = generic.name
  override val phantom: Boolean = generic.isPhantom
  override val capabilities: Set[Capability] = generic.capabilities.flatMap(c => Capability.fromString(c))

  override def attributes: Seq[Attribute] = generic.attributes
}
