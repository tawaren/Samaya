package mandalac.plugin.impl.inter.json

import mandalac.structure.meta.RiskAttribute
import mandalac.structure.Risk

case class RiskImpl(risk: JsonModel.Risk) extends Risk{
  override val index: Int = risk.offset
  override val name: String = risk.name
  override def attributes: Seq[RiskAttribute] = ???
}
