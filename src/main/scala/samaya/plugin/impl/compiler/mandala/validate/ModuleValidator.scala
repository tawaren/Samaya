package samaya.plugin.impl.compiler.mandala.validate

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.structure.{CompiledModule, FunctionSig, Module, Package}


object ModuleValidator {

  def validateModule(module: Module): Unit = {
    module.functions.foreach(f => checkSig(f,"Function"))
    module.signatures.foreach(s => checkSig(s,"Signature"))
    module.implements.foreach(i => checkSig(i,"Implement"))
    //Todo: Validate Instances exist?
  }

  def validateCompiledModule(module: CompiledModule): Unit = {
    validateModule(module);
    module.implements.foreach(i => {
      var hit = false
      for(param <- i.sigParamBindings){
        if(param.attributes.exists(a => a.name == MandalaCompiler.Implicit_Attribute_Name)){
          hit = true
        } else if(hit) {
          feedback(LocatedMessage(s"All implicit bindings must be placed at the end of an Implement",param.src, Error))
        }
      }
    })
  }

  private def checkSig(sig:FunctionSig, kind:String): Unit = {
    var hit = false
    for(param <- sig.params){
      if(param.attributes.exists(a => a.name == MandalaCompiler.Implicit_Attribute_Name)){
        hit = true
      } else if(hit) {
        feedback(LocatedMessage(s"All implicit params must be placed at the end of a $kind",param.src, Error))
      }
    }
  }

}
