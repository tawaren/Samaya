package samaya.plugin.impl.index

import samaya.plugin.service.{ContentLocationIndexer, Selectors}
import samaya.types.{ContentAddressable, Directory}

class NoContentIndexer extends ContentLocationIndexer{
  override def matches(s: Selectors.ContentSelector): Boolean = true
  override def indexContent(content: ContentAddressable): Boolean = false
  override def storeIndex(rootDirectory: Directory): Boolean = false
}
