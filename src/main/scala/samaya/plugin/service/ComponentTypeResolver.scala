package samaya.plugin.service

import samaya.plugin.service.AddressResolver.select
import samaya.plugin.service.category.ComponentTypeResolverPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.Component

import scala.reflect.ClassTag

//A plugin interface to resolve Content & Location Addresses
trait ComponentTypeResolver extends Plugin{
  override type Selector = Selectors.ComponentTypeSelector
  def resolveType(classifiers:Set[String]):Option[Component.ComponentType]
  def resolveType(comp:Component):Option[Component.ComponentType]

}

object ComponentTypeResolver extends ComponentTypeResolver with PluginProxy{

  type PluginType = ComponentTypeResolver
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = ComponentTypeResolverPluginCategory

  def resolveType(classifiers: Set[String]): Option[Component.ComponentType] = {
    select(Selectors.ByClassifier(classifiers)).flatMap(r => r.resolveType(classifiers))
  }

  def resolveType(comp: Component): Option[Component.ComponentType] = {
    select(Selectors.ByComponent(comp)).flatMap(r => r.resolveType(comp))
  }

}
