package samaya.plugin.service.category

import samaya.plugin.service.{ContentLocationIndexer, PluginCategory}

object ContentLocationIndexerPluginCategory extends PluginCategory[ContentLocationIndexer]{
  override def name: String = "content_indexer"
  override def interface: Class[ContentLocationIndexer] = classOf[ContentLocationIndexer]
}
