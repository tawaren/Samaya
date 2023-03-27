package samaya.plugin.impl.validator

import samaya.compilation.ErrorManager._
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.impl.validator.BasicComponentValidator.validation
import samaya.plugin.service.{ComponentValidator, Selectors}
import samaya.structure.{CompiledModule, CompiledTransaction, Component, Module, Package, Transaction}
import samaya.types.Context
import samaya.validation.{CodeValidator, ModuleValidator}

object BasicComponentValidator extends ConfigPluginCompanion {
  val validation : ConfigValue[Boolean] = opt("component.validation|validation").default(true)
    .warnIfFalse("Validation of components is disabled",Compiler())
}

class BasicComponentValidator extends ComponentValidator {

  override def matches(s: Selectors.ValidatorSelector): Boolean = true

  def validateComponent(cmp: Component, pkg: Package): Unit = {
    if(!validation.value) return
    if(cmp.name.isEmpty || cmp.name.charAt(0).isLower) {
      feedback(LocatedMessage(s"Component name ${pkg.name}.${cmp.name} must start with an uppercase Character", cmp.src, Error, Checking()))
    }
    cmp match {
      case cModule: CompiledModule => validateCompiledModule(cModule, pkg)
      case cTransaction: CompiledTransaction => validateCompiledTransaction(cTransaction, pkg)

      //In case it is virtual it may not have the CompiledModule Supertype (& consequently no function bodies)
      case module: Module => ModuleValidator.validateModule(module, pkg)
      case txt: Transaction =>
        val context = Context(pkg)
        ModuleValidator.validateFunction(txt.src, txt, context, "Transaction")
      case _ =>
    }
  }

  private def validateCompiledTransaction(txt: CompiledTransaction, pkg: Package): Unit = {
    val context = Context(pkg)
    //Todo: Validate extra stuff
    //      Like params must be top or primitive <- may get lifted

    //Note: certain things are checked implcitly by not allowing to define them
    //      Like public call accessability
    //      or no generics

    ModuleValidator.validateFunction(txt.src, txt, context, "Transaction")
    CodeValidator.validateCode(Left(txt), context, systemMode = false)
  }

  private def validateCompiledModule(module: CompiledModule, pkg: Package): Unit = {
    ModuleValidator.validateModule(module, pkg)
    CodeValidator.validateModuleCode(module, pkg)
  }

}
