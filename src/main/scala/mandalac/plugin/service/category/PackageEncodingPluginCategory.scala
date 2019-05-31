package mandalac.plugin.service.category

import mandalac.plugin.service.{PackageManager, PluginCategory}

object PackageEncodingPluginCategory extends PluginCategory[PackageManager]{
  override def name: String = "package_resolver"
  override def interface: Class[PackageManager] = classOf[PackageManager]
}
