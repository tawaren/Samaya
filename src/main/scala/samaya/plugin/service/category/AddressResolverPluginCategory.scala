package samaya.plugin.service.category

import samaya.plugin.service.{AddressResolver, PluginCategory}

object AddressResolverPluginCategory extends PluginCategory[AddressResolver]{
  override def name: String = "address_resolver"
  override def interface: Class[AddressResolver] = classOf[AddressResolver]
}
