package samaya.plugin.service

import samaya.plugin.service.DependenciesImportSourceEncoder.select
import samaya.plugin.service.category.{DependenciesEncodingPluginCategory, RepositoriesEncodingPluginCategory}
import samaya.plugin.shared.repositories.Repositories.Repository
import samaya.plugin.{Plugin, PluginProxy}
import samaya.types.InputSource

import scala.reflect.ClassTag


//a plugin description for managing (parsing and validating) interface descriptions
trait RepositoriesImportSourceEncoder extends Plugin {

  override type Selector = Selectors.RepositoriesImportSelector

  def deserializeRepositoriesSources(file:InputSource):Option[Seq[Repository]]
}

object RepositoriesImportSourceEncoder extends RepositoriesImportSourceEncoder with PluginProxy{

  val repositoriesExtensionPrefix = "reps"

  object RepositoriesExtension {
    def apply(format: String): String = repositoriesExtensionPrefix + "." + format
    def unapply(ext: String): Option[String] = if(!ext.startsWith(repositoriesExtensionPrefix)) {
      None
    } else {
      Some(ext.drop(repositoriesExtensionPrefix.length + 1))
    }
    def unapply(source: InputSource): Option[String] = source.identifier.extension.flatMap(unapply)
  }

  type PluginType = RepositoriesImportSourceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = RepositoriesEncodingPluginCategory

  def deserializeRepositoriesSources(source:InputSource):Option[Seq[Repository]] = {
    select(Selectors.RepositoriesDeserializationSelector(source)).flatMap(r => r.deserializeRepositoriesSources(source))
  }
}

