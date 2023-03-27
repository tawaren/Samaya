package samaya.plugin.service

import samaya.plugin.service.category.ContentRepositoryLoaderPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.toolbox.helpers.ConcurrentStrongComputationCache
import samaya.types.Repository.AddressableRepository
import samaya.types._

import scala.reflect.ClassTag

//A plugin interface to resolve Contents
trait ContentRepositoryEncoder extends Plugin{
  override type Selector = Selectors.RepositoryEncoderSelector
  def loadRepository(source:GeneralSource):Option[AddressableRepository]
  def storeRepository(source:GeneralSink, repository:RepositoryBuilder):Boolean
}

object ContentRepositoryEncoder extends ContentRepositoryEncoder with PluginProxy{

  val computationCache = new ConcurrentStrongComputationCache[GeneralSource,Option[AddressableRepository]]()

  type PluginType = ContentRepositoryEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = ContentRepositoryLoaderPluginCategory


  override def storeRepository(sink: GeneralSink, repository:RepositoryBuilder): Boolean = {
    select(Selectors.CreateRepository(sink)).exists(r => r.storeRepository(sink, repository))
  }

  override def loadRepository(source:GeneralSource): Option[AddressableRepository] = {
    computationCache.getOrElseUpdate(source){
      select(Selectors.LoadRepository(source)).flatMap(r => r.loadRepository(source))
    }
  }

  def isRepository(source: GeneralSource): Boolean = {
    matches(Selectors.LoadRepository(source))
  }


}





