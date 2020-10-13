package samaya.plugin

import samaya.compilation.ErrorManager._
import samaya.plugin.service.PluginCategory

//A interfaces for plugins that can check if a certain task can be handled by a specific plugin
trait PluginProxy {
  this: Plugin =>
  type PluginType <: Plugin
  def category:PluginCategory[PluginType]

  protected def select(sel:PluginType#Selector):Option[PluginType] =  {
    val res = PluginManager.getPlugin(category, sel)
    if(res.isEmpty) {
      feedback(PlainMessage(s"No Plugin found satisfying selector: $sel", Warning))
    }
    res
  }

  protected def selectAll(sel:PluginType#Selector):Seq[PluginType] = {
    val res = PluginManager.getPlugins(category, sel)
    if(res.isEmpty) {
      feedback(PlainMessage(s"No Plugin found satisfying selector: $sel", Warning))
    }
    res
  }

  def matches(sel: PluginType#Selector): Boolean = select(sel).isDefined

}
