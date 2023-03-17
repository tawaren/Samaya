package samaya.plugin.service

import samaya.plugin.service.AddressResolver.PluginType
import samaya.plugin.service.category.WorkspaceEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.types.{Directory, GeneralSource, InputSource, Workspace}

import scala.reflect.ClassTag


//a plugin description for managing (parsing and validating) interface descriptions
trait WorkspaceEncoder extends Plugin {

  override type Selector = Selectors.WorkspaceSelector

  //parses file and validate it and then returns the corresponding module
  // beside returning it it is registered in the Module Registry if it is valid
  // as it is just the interface the code of the function body as well as private functions are not present
  def decodeWorkspace(source: GeneralSource):Option[Workspace]
}

object WorkspaceEncoder extends WorkspaceEncoder with PluginProxy{

  val workspaceExtensionPrefix = "wsp"

  type PluginType = WorkspaceEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = WorkspaceEncodingPluginCategory

  override def decodeWorkspace(source: GeneralSource): Option[Workspace] = {
    select(Selectors.WorkspaceDecoderSelector(source)).flatMap(r => r.decodeWorkspace(source))
  }
}
