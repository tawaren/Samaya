package samaya.plugin.impl.inter.json

import samaya.structure.types.Type
import samaya.structure.{Attribute, Result}

case class ReturnImpl(override val location: JsonLocation, ret: JsonModel.Return, override val index: Int) extends Result with JsonSource {
  override val name: String = ret.name
  override val typ: Type = TypeBuilder.toType(ret.typ, location.descendProperty("typ"))
  override def attributes: Seq[Attribute] = ret.attributes
}
