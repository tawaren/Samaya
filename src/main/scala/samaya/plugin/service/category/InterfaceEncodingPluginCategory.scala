package samaya.plugin.service.category

import samaya.plugin.service.{InterfaceEncoder, PluginCategory}

object InterfaceEncodingPluginCategory extends PluginCategory[InterfaceEncoder]{
  override def name: String = "interface_resolver"
  override def interface: Class[InterfaceEncoder] = classOf[InterfaceEncoder]
}
