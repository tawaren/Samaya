package samaya.plugin.service

import samaya.plugin.{Plugin, PluginProxy}
import samaya.plugin.service.category.{ComponentValidatorPluginCategory}
import samaya.structure.{Component, Package}

trait ComponentValidator extends Plugin{

  override type Selector = Selectors.ValidatorSelector

  def validateComponent(cmp:Component, pkg:Package): Unit
}

object ComponentValidator extends ComponentValidator with PluginProxy{

  type PluginType = ComponentValidator
  override def category: PluginCategory[PluginType] = ComponentValidatorPluginCategory

  def validateComponent(cmp:Component, pkg:Package): Unit = {
    selectAll(Selectors.ValidatorSelector(cmp)).foreach(r => r.validateComponent(cmp, pkg))
  }

}