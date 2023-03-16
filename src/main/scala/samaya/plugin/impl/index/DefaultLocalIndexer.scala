package samaya.plugin.impl.index

import samaya.plugin.service.{ContentLocationIndexer, Selectors}
import samaya.plugin.shared.index.CachedRepository
import samaya.structure.ContentAddressable
import samaya.types.Directory

class DefaultLocalIndexer extends ContentLocationIndexer{
  override def matches(s: Selectors.ContentSelector): Boolean = s match {
    case Selectors.UpdateContentIndex => true
    case _ => false
  }

  override def indexContent(content: ContentAddressable): Boolean = {
    CachedRepository.repo.put(content.hash, content)
    true
  }

  override def storeIndex(rootDirectory: Directory): Boolean = false
}
