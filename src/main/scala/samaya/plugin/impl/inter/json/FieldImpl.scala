package samaya.plugin.impl.inter.json

import samaya.structure.types.Type
import samaya.structure.{Attribute, Field}

case class FieldImpl(override val location: JsonLocation, field: JsonModel.Field, override val pos: Int) extends Field with JsonSource {
  override val name: String = field.name
  override val typ: Type = TypeBuilder.toType(field.typ, location.descendProperty("typ"))

  override def attributes: Seq[Attribute] = field.attributes
}
