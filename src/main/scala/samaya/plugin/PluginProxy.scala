package samaya.plugin

import samaya.compilation.ErrorManager._
import samaya.plugin.service.PluginCategory

import scala.reflect.ClassTag

//A interfaces for plugins that can check if a certain task can be handled by a specific plugin
trait PluginProxy {
  this: Plugin =>
  type PluginType <: Plugin
  def category:PluginCategory[PluginType]
  implicit def classTag: ClassTag[PluginType]

  protected def select(sel:PluginType#Selector, silent: Boolean = false):Option[PluginType] =  {
    val res = PluginManager.getPlugin(category, sel)
    if(res.isEmpty && !silent) {
      feedback(PlainMessage(s"No Plugin found satisfying selector: $sel", Warning, Always))
    }
    res
  }

  protected def selectAll(sel:PluginType#Selector):Seq[PluginType] = {
    val res = PluginManager.getPlugins(category, sel)
    if(res.isEmpty) {
      feedback(PlainMessage(s"No Plugin found satisfying selector: $sel", Warning, Always))
    }
    res
  }

  def matches(sel: PluginType#Selector): Boolean = select(sel).isDefined

}
