package samaya.plugin.service.category

import samaya.plugin.service.{PluginCategory, ReferenceResolver}

object ReferenceResolverPluginCategory extends PluginCategory[ReferenceResolver]{
  override def name: String = "reference_resolver"
  override def interface: Class[ReferenceResolver] = classOf[ReferenceResolver]
}
