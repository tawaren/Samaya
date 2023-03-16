package samaya.plugin.service.category

import samaya.plugin.service.{ContentRepositoryLoader, PluginCategory}

object ContentRepositoryLoaderPluginCategory extends PluginCategory[ContentRepositoryLoader]{
  override def name: String = "repository_loader"
  override def interface: Class[ContentRepositoryLoader] = classOf[ContentRepositoryLoader]
}
