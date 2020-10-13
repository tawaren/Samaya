package samaya.plugin.impl.inter.json

import samaya.structure.types.Type
import samaya.structure.{Attribute, Param}

case class ParamImpl(override val location: JsonLocation, param: JsonModel.Param, override val index: Int) extends Param with JsonSource {
  override val name: String = param.name
  override val typ: Type = TypeBuilder.toType(param.typ)
  override val consumes: Boolean = param.isConsumed

  override def attributes: Seq[Attribute] = param.attributes
}
