package samaya.plugin.service.category

import samaya.plugin.service.{ComponentTypeResolver, PluginCategory}

object ComponentTypeResolverPluginCategory extends PluginCategory[ComponentTypeResolver]{
  override def name: String = "component_type_resolver"
  override def interface: Class[ComponentTypeResolver] = classOf[ComponentTypeResolver]
}
