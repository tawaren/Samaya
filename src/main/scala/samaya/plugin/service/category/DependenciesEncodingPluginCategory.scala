package samaya.plugin.service.category

import samaya.plugin.service.{PluginCategory, DependenciesImportSourceEncoder}

object DependenciesEncodingPluginCategory extends PluginCategory[DependenciesImportSourceEncoder]{
  override def name: String = "dependencies_resolver"
  override def interface: Class[DependenciesImportSourceEncoder] = classOf[DependenciesImportSourceEncoder]
}
