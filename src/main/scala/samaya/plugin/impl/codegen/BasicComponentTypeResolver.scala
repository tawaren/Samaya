package samaya.plugin.impl.codegen

import samaya.plugin.service.{ComponentTypeResolver, Selectors}
import samaya.structure.Component

class BasicComponentTypeResolver extends ComponentTypeResolver{

  private val handledClassifier = Set(Component.MODULE.classifier,Component.TRANSACTION.classifier)

  override def matches(s: Selectors.ComponentTypeSelector): Boolean = s match {
    case Selectors.ByClassifier(classifiers) => classifiers.intersect(handledClassifier).nonEmpty
    case Selectors.ByComponent(component) if !component.isVirtual => component.classifier.intersect(handledClassifier).nonEmpty
    case _ => false
  }

  override def resolveType(classifiers: Set[String]): Option[Component.ComponentType] = {
    if(classifiers.contains(Component.MODULE.classifier)) return Some(Component.MODULE)
    if(classifiers.contains(Component.TRANSACTION.classifier)) return Some(Component.TRANSACTION)
    None
  }

  override def resolveType(comp: Component): Option[Component.ComponentType] = resolveType(comp.classifier)
}
