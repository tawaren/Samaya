package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.FieldAttribute
import mandalac.structure.types.Type
import mandalac.structure.Field

case class FieldImpl(field: JsonModel.Field, override val pos: Int) extends Field{
  override val name: String = field.name
  override val typ: Type = TypeBuilder.toType(field.typ)

  override def attributes: List[FieldAttribute] = ???
}
