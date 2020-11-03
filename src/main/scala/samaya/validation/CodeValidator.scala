package samaya.validation

import samaya.compilation.ErrorManager
import samaya.structure.{CompiledModule, CompiledTransaction, FunctionDef, ImplementDef, Module, Package}
import samaya.toolbox.checks.{AccessibilityChecker, ArityChecker, CaseOrderChecker, DefinitionChecker, IdAvailabilityChecker, SubStructuralCapabilityChecker, TransactionSemanticChecker, TypeChecker, UsageChecker}
import samaya.types.Context
import samaya.toolbox.traverse.ViewTraverser


object CodeValidator {

  def validateModuleCode(current:CompiledModule, pkg:Package):Boolean = {
    val context = Context(current,pkg)
    var res = true

    for(f <- current.functions) {
      res |= validateCode(Left(f), context, current.mode != Module.Normal)
    }

    for(i <- current.implements) {
      res |= validateCode(Right(i), context, current.mode != Module.Normal)
    }

    res
  }

  def validateTransactionCode(current:CompiledTransaction, pkg:Package):Boolean = {
    val context = Context(pkg)
    validateCode(Left(current), context, systemMode = false)
  }

  def validateCode(comp:Either[FunctionDef,ImplementDef], code_context:Context, systemMode:Boolean):Boolean = {
    ErrorManager.producesErrorValue{
      val checker = new ViewTraverser
        with DefinitionChecker
        with UsageChecker
        with TypeChecker
        with SubStructuralCapabilityChecker
        with IdAvailabilityChecker
        with AccessibilityChecker
        with CaseOrderChecker
        with TransactionSemanticChecker
        with ArityChecker {
          override def context: Context = code_context
          override def entry: Either[FunctionDef, ImplementDef] = comp
          override def isSystem: Boolean = systemMode
      }
      checker.traverse()
    }.nonEmpty
  }

}
