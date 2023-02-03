package samaya.plugin.impl.indexer

import samaya.plugin.service.{ContentLocationIndexer, Selectors}
import samaya.structure.ContentAddressable
import samaya.types.Directory

class NoContentIndexer extends ContentLocationIndexer{
  override def matches(s: Selectors.ContentSelector): Boolean = true
  override def indexContent(context: Option[Directory], content: ContentAddressable): Boolean = false
}
