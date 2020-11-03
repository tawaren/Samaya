package samaya.plugin.impl.compiler.mandala.entry

import samaya.structure.types.{SourceId, Type}
import samaya.structure.{Generic, TypeParameterized}

case class TypeAlias(
  name:String,
  override val generics:Seq[Generic],
  target:Type,
  source:SourceId
) extends TypeParameterized{
  override def generic(index: Int): Option[Generic] = generics.find(_.index == index)
}