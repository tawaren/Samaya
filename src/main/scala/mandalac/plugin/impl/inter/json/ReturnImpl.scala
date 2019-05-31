package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.ParamAttribute
import mandalac.structure.types.Type
import mandalac.structure.Result

case class ReturnImpl(ret: JsonModel.Return, parent: JsonModel.Function, override val pos: Int) extends Result{
  override val name: String = ret.name
  override val typ: Type = TypeBuilder.toType(ret.typ)

  override def borrows: Set[String] = ret.borrows

  override def attributes: Seq[ParamAttribute] = ???
}
