package samaya.plugin.impl.index

import samaya.plugin.service.{ContentLocationIndexer, Selectors}
import samaya.structure.ContentAddressable
import samaya.types.Directory

class NoContentIndexer extends ContentLocationIndexer{
  override def matches(s: Selectors.ContentSelector): Boolean = true
  override def indexContent(content: ContentAddressable): Boolean = false
  override def storeIndex(rootDirectory: Directory): Boolean = false
}
