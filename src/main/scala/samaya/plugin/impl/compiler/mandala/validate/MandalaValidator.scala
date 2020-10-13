package samaya.plugin.impl.compiler.mandala.validate

import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, ImplInstance, Instance}
import samaya.plugin.impl.compiler.mandala.components.clazz.Class
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.service.{ComponentValidator, Selectors}
import samaya.structure.{CompiledModule, Component, Module, Package}


class MandalaValidator extends ComponentValidator {

  override def matches(s: Selectors.ValidatorSelector): Boolean = {
    s match {
      //Note: For classes the standard module validator suffices
      case Selectors.ValidatorSelector(_: DefInstance) => true
      case Selectors.ValidatorSelector(_: MandalaModule) => true
      case _ => false
    }
  }

  //Validates the stuff not validate by standard module validator
  // in case of the DefInstance we validate everithing as it is not a Module
  override def validateComponent(cmp: Component, pkg: Package): Unit = {
    cmp match {
      case inst: DefInstance => InstanceValidator.validateDefInstance(inst, pkg)
      case inst: ImplInstance => InstanceValidator.validateInstance(inst, pkg)
      case module: CompiledModule with MandalaModule => ModuleValidator.validateCompiledModule(module)
      case module: MandalaModule => ModuleValidator.validateModule(module)
    }
  }

}
