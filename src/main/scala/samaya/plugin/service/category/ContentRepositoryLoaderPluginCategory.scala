package samaya.plugin.service.category

import samaya.plugin.service.{ContentRepositoryEncoder, PluginCategory}

object ContentRepositoryLoaderPluginCategory extends PluginCategory[ContentRepositoryEncoder]{
  override def name: String = "repository_loader"
  override def interface: Class[ContentRepositoryEncoder] = classOf[ContentRepositoryEncoder]
}
