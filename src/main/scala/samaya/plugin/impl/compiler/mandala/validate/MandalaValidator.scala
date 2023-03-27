package samaya.plugin.impl.compiler.mandala.validate

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Builder, Compiler, PlainMessage}
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, ImplInstance, Instance}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.mandala.validate.MandalaValidator.validation
import samaya.plugin.service.{ComponentValidator, Selectors}
import samaya.structure.{CompiledModule, Component, Package}

object MandalaValidator extends ConfigPluginCompanion {
  private val validation : ConfigValue[Boolean] = opt("mandala.component.validation|component.validation|validation").default(true)
    .warnIfFalse("Validation of Mandala modules and instances is disabled",Compiler())
}

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
    if(!validation.value) return
    cmp match {
      case inst: DefInstance => InstanceValidator.validateDefInstance(inst, pkg)
      case inst: ImplInstance => InstanceValidator.validateInstance(inst, pkg)
      case module: CompiledModule with MandalaModule => ModuleValidator.validateCompiledModule(module,pkg)
      case module: MandalaModule => ModuleValidator.validateModule(module,pkg)
      //Todo: Validate SigClass matches FunClass
    }
  }

}
