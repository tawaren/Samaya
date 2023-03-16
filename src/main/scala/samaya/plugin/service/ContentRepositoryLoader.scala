package samaya.plugin.service

import samaya.plugin.service.ContentLocationIndexer.select
import samaya.plugin.service.PackageEncoder.deserializePackage
import samaya.plugin.service.category.{ContentLocationIndexerPluginCategory, ContentRepositoryLoaderPluginCategory}
import samaya.plugin.{Plugin, PluginProxy}
import samaya.plugin.shared.repositories.Repositories.Repository
import samaya.structure.{ContentAddressable, LinkablePackage}
import samaya.types._

import scala.reflect.ClassTag

//A plugin interface to resolve Contents
trait ContentRepositoryLoader extends Plugin{
  override type Selector = Selectors.RepositoryLoaderSelector
  def loadRepository(source:InputSource):Option[Repository]
}

object ContentRepositoryLoader extends ContentRepositoryLoader with PluginProxy{

  object Loader extends AddressResolver.Loader[Repository] {
    override def load(src: InputSource): Option[Repository] = loadRepository(src)
    override def tag: ClassTag[Repository] = implicitly[ClassTag[Repository]]
  }

  type PluginType = ContentRepositoryLoader
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = ContentRepositoryLoaderPluginCategory

  override def loadRepository(source:InputSource): Option[Repository] = {
    select(Selectors.LoadRepository(source)).flatMap(r => r.loadRepository(source))
  }

}





