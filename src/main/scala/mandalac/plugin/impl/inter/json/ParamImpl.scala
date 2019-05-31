package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.ParamAttribute
import mandalac.structure.types.Type
import mandalac.structure.Param

case class ParamImpl(param: JsonModel.Param, override val pos: Int) extends Param{
  override val name: String = param.name
  override val typ: Type = TypeBuilder.toType(param.typ)
  override val consumes: Boolean = param.isConsumed

  override def attributes: Seq[ParamAttribute] = ???
}
