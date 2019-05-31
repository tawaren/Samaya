package mandalac.plugin.service.category

import mandalac.plugin.service.{InterfaceManager, PluginCategory}

object InterfaceEncodingPluginCategory extends PluginCategory[InterfaceManager]{
  override def name: String = "interface_resolver"
  override def interface: Class[InterfaceManager] = classOf[InterfaceManager]
}
