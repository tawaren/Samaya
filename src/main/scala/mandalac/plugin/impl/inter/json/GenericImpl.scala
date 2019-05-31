package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.GenericAttribute
import mandalac.structure.types.Capability
import mandalac.structure.Generic

case class GenericImpl(generic: JsonModel.Generic, override val pos:Int) extends Generic{
  override val name: String = generic.name
  override val protection: Boolean = generic.isProtected
  override val phantom: Boolean = generic.isPhantom
  override val capabilities: Set[Capability] = generic.capabilities.flatMap(c => Capability.fromString(c))

  override def attributes: List[GenericAttribute] = ???
}
