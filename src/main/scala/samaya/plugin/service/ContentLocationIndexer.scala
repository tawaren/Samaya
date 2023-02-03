package samaya.plugin.service

import samaya.plugin.service.category.ContentLocationIndexerPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.{ContentAddressable, LinkablePackage}
import samaya.structure.types.Hash
import samaya.types._

//A plugin interface to resolve Contents
trait ContentLocationIndexer extends Plugin{
  override type Selector = Selectors.ContentSelector
  def indexContent(context:Option[Directory], content:ContentAddressable):Boolean
}


object ContentLocationIndexer extends ContentLocationIndexer with PluginProxy{

  type PluginType = ContentLocationIndexer
  override def category: PluginCategory[PluginType] = ContentLocationIndexerPluginCategory

  def indexContent(context:Option[Directory], content:ContentAddressable):Boolean = {
    select(Selectors.UpdateContentIndex(context)).exists(r => r.indexContent(context, content))
  }
}



