package samaya.plugin.service.category

import samaya.plugin.service.{PackageEncoder, PluginCategory}

object PackageEncodingPluginCategory extends PluginCategory[PackageEncoder]{
  override def name: String = "package_resolver"
  override def interface: Class[PackageEncoder] = classOf[PackageEncoder]
}
