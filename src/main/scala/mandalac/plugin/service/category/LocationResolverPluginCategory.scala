package mandalac.plugin.service.category

import mandalac.plugin.service.{LocationResolver, PluginCategory}

object LocationResolverPluginCategory extends PluginCategory[LocationResolver]{
  override def name: String = "location_resolver"
  override def interface: Class[LocationResolver] = classOf[LocationResolver]
}
