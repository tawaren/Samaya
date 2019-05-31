package mandalac.plugin.service.category

import mandalac.plugin.service.{PluginCategory, WorkspaceManager}

object WorkspaceEncodingPluginCategory extends PluginCategory[WorkspaceManager]{
  override def name: String = "workspace_resolver"
  override def interface: Class[WorkspaceManager] = classOf[WorkspaceManager]
}
