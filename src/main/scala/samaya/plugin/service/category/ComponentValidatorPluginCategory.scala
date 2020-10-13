package samaya.plugin.service.category

import samaya.plugin.service.{ComponentValidator, PluginCategory}

object ComponentValidatorPluginCategory extends PluginCategory[ComponentValidator]{
  override def name: String = "component_validator"
  override def interface: Class[ComponentValidator] = classOf[ComponentValidator]
}
