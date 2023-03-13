package samaya.plugin.service

import java.io.OutputStream
import samaya.plugin.service.category.DebugAssemblerPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.{Component, Package}

import scala.reflect.ClassTag


trait DebugAssembler extends Plugin {
  override type Selector = Selectors.DebugAssemblerSelector
  def serializeComponent(pkg:Package, cmp:Component, out:OutputStream): Unit
}

object DebugAssembler extends DebugAssembler with PluginProxy{

  type PluginType = DebugAssembler
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = DebugAssemblerPluginCategory
  override def serializeComponent(pkg:Package, cmp:Component, out:OutputStream): Unit  = {
    select(Selectors.DebugAssemblerSelector(cmp)).foreach(r => r.serializeComponent(pkg, cmp, out))
  }

}
