package samaya.plugin.service

import samaya.plugin.service.category.RepositoriesEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.types.{GeneralSource, InputSource, Repository}

import scala.reflect.ClassTag


//a plugin description for managing (parsing and validating) interface descriptions
trait RepositoriesImportSourceEncoder extends Plugin {
  override type Selector = Selectors.RepositoriesImportSelector
  def decodeRepositoriesSources(source: GeneralSource):Option[Seq[Repository]]
}

object RepositoriesImportSourceEncoder extends RepositoriesImportSourceEncoder with PluginProxy{

  val repositoriesExtensionPrefix = "reps"

  type PluginType = RepositoriesImportSourceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = RepositoriesEncodingPluginCategory

  def decodeRepositoriesSources(source: GeneralSource):Option[Seq[Repository]] = {
    select(Selectors.RepositoriesDecoderSelector(source)).flatMap(r => r.decodeRepositoriesSources(source))
  }
}

