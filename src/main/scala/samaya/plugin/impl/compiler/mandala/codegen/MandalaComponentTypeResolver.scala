package samaya.plugin.impl.compiler.mandala.codegen

import samaya.compilation.ErrorManager.Compiler
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.MandalaComponent
import samaya.plugin.service.{ComponentTypeResolver, Selectors}
import samaya.structure.Component

class MandalaComponentTypeResolver extends ComponentTypeResolver{

  private val handledClassifier = Set(Component.MODULE.classifier,Component.TRANSACTION.classifier)

  override def matches(s: Selectors.ComponentTypeSelector): Boolean = s match {
    case Selectors.ByClassifier(classifiers) => classifiers.contains(MandalaCompiler.Language)
    case Selectors.ByComponent(component) if !component.isVirtual => component.isInstanceOf[MandalaComponent];
    case _ => false
  }

  override def resolveType(classifiers: Set[String]): Option[Component.ComponentType] = {
    if(classifiers.subsetOf(MandalaCompiler.MandalaModule_Classifier)) return Some(Component.MODULE)
    if(classifiers.subsetOf(MandalaCompiler.SigClass_Classifier)) return Some(Component.MODULE)
    if(classifiers.subsetOf(MandalaCompiler.ImplInstance_Classifier)) return Some(Component.MODULE)
    if(classifiers.subsetOf(MandalaCompiler.Transaction_Classifier)) return Some(Component.TRANSACTION)
    //if(classifiers.subsetOf(MandalaCompiler.FunClass_Classifier)) return None //is virtual
    //if(classifiers.subsetOf(MandalaCompiler.DefInstance_Classifier)) return None //is virtual
    None
  }

  override def resolveType(comp: Component): Option[Component.ComponentType] = resolveType(comp.classifier)
}
