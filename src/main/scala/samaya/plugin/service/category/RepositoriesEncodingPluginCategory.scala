package samaya.plugin.service.category

import samaya.plugin.service.{DependenciesImportSourceEncoder, PluginCategory, RepositoriesImportSourceEncoder}

object RepositoriesEncodingPluginCategory extends PluginCategory[RepositoriesImportSourceEncoder]{
  override def name: String = "repositories_resolver"
  override def interface: Class[RepositoriesImportSourceEncoder] = classOf[RepositoriesImportSourceEncoder]
}
