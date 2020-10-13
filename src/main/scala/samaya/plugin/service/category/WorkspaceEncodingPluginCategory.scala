package samaya.plugin.service.category

import samaya.plugin.service.{PluginCategory, WorkspaceEncoder}

object WorkspaceEncodingPluginCategory extends PluginCategory[WorkspaceEncoder]{
  override def name: String = "workspace_resolver"
  override def interface: Class[WorkspaceEncoder] = classOf[WorkspaceEncoder]
}
