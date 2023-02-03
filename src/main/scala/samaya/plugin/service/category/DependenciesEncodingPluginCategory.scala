package samaya.plugin.service.category

import samaya.plugin.service.{PluginCategory, DependenciesEncoder}

object DependenciesEncodingPluginCategory extends PluginCategory[DependenciesEncoder]{
  override def name: String = "dependencies_resolver"
  override def interface: Class[DependenciesEncoder] = classOf[DependenciesEncoder]
}
