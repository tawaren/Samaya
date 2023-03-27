package samaya.plugin

import samaya.compilation.ErrorManager._
import samaya.plugin.PluginProxy.{resolveSilent, silent}
import samaya.plugin.service.PluginCategory

import scala.reflect.ClassTag
import scala.util.DynamicVariable

//A interfaces for plugins that can check if a certain task can be handled by a specific plugin
trait PluginProxy {
  this: Plugin =>
  type PluginType <: Plugin
  def category:PluginCategory[PluginType]
  implicit def classTag: ClassTag[PluginType]

  protected def select(sel:PluginType#Selector):Option[PluginType] =  {
    val res = PluginManager.getPlugin(category, sel)
    if(res.isEmpty && !silent.value) {
      feedback(PlainMessage(s"No Plugin found satisfying selector: $sel", Warning, Always))
    }
    res
  }

  protected def selectAll(sel:PluginType#Selector):Seq[PluginType] = {
    val res = PluginManager.getPlugins(category, sel)
    if(res.isEmpty && !silent.value) {
      feedback(PlainMessage(s"No Plugin found satisfying selector: $sel", Warning, Always))
    }
    res
  }

  def matches(sel: PluginType#Selector): Boolean = resolveSilent(select(sel).isDefined)

}

object PluginProxy {
  val silent : DynamicVariable[Boolean] = new DynamicVariable(false)

  def resolveSilent[T](f: => T):T = silent.withValue(true)(f)
}
