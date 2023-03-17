package samaya.plugin.service

import samaya.plugin.service.AddressResolver.PluginType
import samaya.plugin.service.category.ContentLocationIndexerPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.LinkablePackage
import samaya.structure.types.Hash
import samaya.types._

import scala.reflect.ClassTag

//A plugin interface to resolve Contents
trait ContentLocationIndexer extends Plugin{
  override type Selector = Selectors.ContentSelector
  def indexContent(content:ContentAddressable):Boolean
  def storeIndex(rootDirectory:Directory):Boolean
}


object ContentLocationIndexer extends ContentLocationIndexer with PluginProxy{

  type PluginType = ContentLocationIndexer
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = ContentLocationIndexerPluginCategory

  def indexContent(content:ContentAddressable):Boolean = {
    select(Selectors.UpdateContentIndex).exists(r => r.indexContent(content))
  }

  def storeIndex(rootDirectory: Directory): Boolean  = {
    select(Selectors.StoreContentIndex(rootDirectory)).exists(r => r.storeIndex(rootDirectory))
  }
}



