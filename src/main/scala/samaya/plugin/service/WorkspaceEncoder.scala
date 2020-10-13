package samaya.plugin.service

import samaya.plugin.service.category.WorkspaceEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.types.{InputSource, Workspace}


//a plugin description for managing (parsing and validating) interface descriptions
trait WorkspaceEncoder extends Plugin{

  override type Selector = Selectors.WorkspaceSelector

  //parses file and validate it and then returns the corresponding module
  // beside returning it it is registered in the Module Registry if it is valid
  // as it is just the interface the code of the function body as well as private functions are not present
  def deserializeWorkspace(file:InputSource):Option[Workspace]
}

object WorkspaceEncoder extends WorkspaceEncoder with PluginProxy{

  val workspaceExtensionPrefix = "wsp"

  object WorkSpaceExtension {
    def apply(format: String): String = workspaceExtensionPrefix + "." + format
    def unapply(ext: String): Option[String] = if(!ext.startsWith(workspaceExtensionPrefix)) {
      None
    } else {
      Some(ext.drop(workspaceExtensionPrefix.length + 1))
    }
    def unapply(source: InputSource): Option[String] = source.identifier.extension.flatMap(unapply)
  }

  type PluginType = WorkspaceEncoder
  override def category: PluginCategory[PluginType] = WorkspaceEncodingPluginCategory

  override def deserializeWorkspace( source: InputSource): Option[Workspace] = {
    select(Selectors.WorkspaceDeserializationSelector(source)).flatMap(r => r.deserializeWorkspace(source))
  }
}
