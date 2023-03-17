package samaya.plugin.service

import samaya.plugin.{Plugin, PluginProxy}
import samaya.plugin.service.category.DependenciesEncodingPluginCategory
import samaya.structure.LinkablePackage
import samaya.types.{GeneralSource, InputSource}

import scala.reflect.ClassTag


//a plugin description for managing (parsing and validating) interface descriptions
trait DependenciesImportSourceEncoder extends Plugin {

  override type Selector = Selectors.DependenciesImportSelector

  def decodeDependenciesSources(source : GeneralSource):Option[Seq[LinkablePackage]]
}

object DependenciesImportSourceEncoder extends DependenciesImportSourceEncoder with PluginProxy{

  val dependenciesExtensionPrefix = "deps"

  type PluginType = DependenciesImportSourceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = DependenciesEncodingPluginCategory

  def decodeDependenciesSources(source : GeneralSource):Option[Seq[LinkablePackage]] = {
    select(Selectors.DependenciesDecoderSelector(source)).flatMap(r => r.decodeDependenciesSources(source))
  }
}
