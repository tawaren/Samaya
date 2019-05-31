package mandalac.plugin

import mandalac.plugin.service.PluginCategory

//A interfaces for plugins that can check if a certain task can be handled by a specific plugin
trait PluginProxy {
  this: Plugin =>
  type PluginType <: Plugin
  def category:PluginCategory[PluginType]
  protected def select(sel:PluginType#Selector):Option[PluginType] =  PluginManager.getPlugin(category, sel)
  protected def selectAll(sel:PluginType#Selector):Seq[PluginType] =  PluginManager.getPlugins(category, sel)
  def matches(sel: PluginType#Selector): Boolean = select(sel).isDefined


}
