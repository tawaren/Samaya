package samaya.plugin.impl.index

import samaya.build.BuildRepository
import samaya.plugin.service.{ContentLocationIndexer, Selectors}
import samaya.types.{ContentAddressable, Directory}

class BuildRepositoryIndexer extends ContentLocationIndexer{
  override def matches(s: Selectors.ContentSelector): Boolean = s match {
    case Selectors.UpdateContentIndex => true
    case _ => false
  }

  override def indexContent(content: ContentAddressable): Boolean = {
    BuildRepository.addEntry(content)
    true
  }

  override def storeIndex(rootDirectory: Directory): Boolean = false
}
