package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.DataTypeAttribute
import mandalac.structure.types.Capability
import mandalac.structure.{Constructor, DataType, Generic}

case class DataTypeImpl(data: JsonModel.Datatype) extends DataType{
  override val index: Int = data.offset
  override val name: String = data.name

  override def attributes: Seq[DataTypeAttribute] = ???

  override val generics: Seq[Generic] = data.generics.zipWithIndex.map(gi => GenericImpl(gi._1, gi._2))
  override def generic(index: Int): Option[Generic] = generics.find(gi => gi.pos == index)
  override val constructors: Seq[Constructor] = TypeBuilder.inContext(generics){
    data.constructors.zipWithIndex.map(ci => ConstructorImpl(ci._1, ci._2))
  }
  override def constructor(tag: Int): Option[Constructor] = constructors.find(c => c.tag == tag)
  override val capabilities: Set[Capability] = data.capabilities.flatMap(c => Capability.fromString(c))
}
