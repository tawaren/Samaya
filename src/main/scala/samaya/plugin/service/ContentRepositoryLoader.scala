package samaya.plugin.service

import samaya.plugin.service.category.ContentRepositoryLoaderPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.types.Repository.AddressableRepository
import samaya.types._

import scala.reflect.ClassTag

//A plugin interface to resolve Contents
trait ContentRepositoryLoader extends Plugin{
  override type Selector = Selectors.RepositoryLoaderSelector
  def loadRepository(source:GeneralSource):Option[AddressableRepository]
}

object ContentRepositoryLoader extends ContentRepositoryLoader with PluginProxy{

  type PluginType = ContentRepositoryLoader
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = ContentRepositoryLoaderPluginCategory

  override def loadRepository(source:GeneralSource): Option[AddressableRepository] = {
    select(Selectors.LoadRepository(source)).flatMap(r => r.loadRepository(source))
  }

}





