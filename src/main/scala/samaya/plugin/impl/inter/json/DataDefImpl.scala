package samaya.plugin.impl.inter.json

import samaya.structure.types.{Accessibility, Capability, Permission}
import samaya.structure.{Attribute, Constructor, DataDef, Generic}

case class DataDefImpl(override val location: JsonLocation, data: JsonModel.DataSignature) extends DataDef with JsonSource {
  override val index: Int = data.offset
  override val name: String = data.name
  override val position: Int = data.position
  override val generics: Seq[Generic] = {
    val genericLoc = location.descendProperty("generics")
    data.generics.zipWithIndex.map(gi => GenericImpl(genericLoc.descendProperty(gi._1.name), gi._1, gi._2))
  }
  override val accessibility: Map[Permission, Accessibility] = data.accessibility.flatMap {
    case (perm, access) =>
      for(p <- Permission.fromString(perm); a <- Accessibility.fromString(access.name,access.guards))
        yield (p,a)
  }

  override val capabilities: Set[Capability] = data.capabilities.flatMap(c => Capability.fromString(c))
  override def attributes: Seq[Attribute] = data.attributes
  override val external: Option[Short] = data.external
  override val top: Boolean = data.top

  override val constructors: Seq[Constructor] = TypeBuilder.inContext(generics){
    val ctrLoc = location.descendProperty("constructors")
    data.constructors.zipWithIndex.map(ci => ConstructorImpl(ctrLoc.descendProperty(ci._1.name),ci._1, ci._2))
  }
}
