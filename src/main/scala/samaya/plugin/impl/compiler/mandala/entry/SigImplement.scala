package samaya.plugin.impl.compiler.mandala.entry

import samaya.structure.{Generic, TypeParameterized}
import samaya.structure.types.{ImplFunc, SourceId, StdFunc}

case class SigImplement(
  name:String,
  override val generics:Seq[Generic],
  funTarget:StdFunc,
  implTarget:ImplFunc,
  source:SourceId
) extends TypeParameterized{
  override def generic(index: Int): Option[Generic] = generics.find(_.index == index)
}