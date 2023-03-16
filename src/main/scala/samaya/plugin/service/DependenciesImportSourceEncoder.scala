package samaya.plugin.service

import samaya.plugin.service.LanguageCompiler.PluginType
import samaya.plugin.{Plugin, PluginProxy}
import samaya.plugin.service.category.DependenciesEncodingPluginCategory
import samaya.structure.LinkablePackage
import samaya.types.InputSource

import scala.reflect.ClassTag


//a plugin description for managing (parsing and validating) interface descriptions
trait DependenciesImportSourceEncoder extends Plugin {

  override type Selector = Selectors.DependenciesImportSelector

  def deserializeDependenciesSources(file:InputSource):Option[Seq[LinkablePackage]]
}

object DependenciesImportSourceEncoder extends DependenciesImportSourceEncoder with PluginProxy{

  val dependenciesExtensionPrefix = "deps"

  object DependenciesExtension {
    def apply(format: String): String = dependenciesExtensionPrefix + "." + format
    def unapply(ext: String): Option[String] = if(!ext.startsWith(dependenciesExtensionPrefix)) {
      None
    } else {
      Some(ext.drop(dependenciesExtensionPrefix.length + 1))
    }
    def unapply(source: InputSource): Option[String] = source.identifier.extension.flatMap(unapply)
  }

  type PluginType = DependenciesImportSourceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = DependenciesEncodingPluginCategory

  def deserializeDependenciesSources(source:InputSource):Option[Seq[LinkablePackage]] = {
    select(Selectors.DependenciesDeserializationSelector(source)).flatMap(r => r.deserializeDependenciesSources(source))
  }
}
