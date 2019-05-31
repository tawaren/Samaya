package mandalac.plugin.service

import mandalac.plugin.service.category.WorkspaceEncodingPluginCategory
import mandalac.plugin.{Plugin, PluginProxy}
import mandalac.types.{InputSource, Workspace}


//a plugin description for managing (parsing and validating) interface descriptions
trait WorkspaceManager extends Plugin{

  override type Selector = Selectors.WorkspaceSelector

  //parses file and validate it and then returns the corresponding module
  // beside returning it it is registered in the Module Registry if it is valid
  // as it is just the interface the code of the function body as well as private functions are not present
  //todo: make a simple deserialize and do the rest in a common part
  def deserializeWorkspace(file:InputSource):Option[Workspace]
}

object WorkspaceManager extends WorkspaceManager with PluginProxy{

  type PluginType = WorkspaceManager
  override def category: PluginCategory[PluginType] = WorkspaceEncodingPluginCategory

  override def deserializeWorkspace(source: InputSource): Option[Workspace] = {
    select(Selectors.WorkspaceDeserializationSelector(source)).flatMap(r => r.deserializeWorkspace(source))
  }
}
