package samaya.plugin.impl.compiler.mandala.validate

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.mandala.entry.TypeAlias
import samaya.structure.{CompiledModule, FunctionSig, Module, Package}
import samaya.types.Context
import samaya.validation.SignatureValidator


object ModuleValidator {

  def validateModule(module: MandalaModule, pkg:Package): Unit = {
    val context = Context(module, pkg)
    module.functions.foreach(f => checkSig(f,"Function"))
    module.signatures.foreach(s => checkSig(s,"Signature"))
    module.implements.foreach(i => checkSig(i,"Implement"))
    module.typeAlias.foreach{
      case ta@TypeAlias(name, generics, target, source) =>
        val genericIdents:Set[String] = generics.map(p => p.name).toSet
        //check that type param names are unique
        if(genericIdents.size != generics.size){
          feedback(LocatedMessage(s"$name generics must have unique names", source, Error, Checking()))
        }
        SignatureValidator.validateType(target.src, target, ta, context)

    }
    //Todo: Validate Instances exist?
  }

  def validateCompiledModule(module: CompiledModule with MandalaModule, pkg:Package): Unit = {
    validateModule(module, pkg)
    module.implements.foreach(i => {
      var hit = false
      for(param <- i.sigParamBindings){
        if(param.attributes.exists(a => a.name == MandalaCompiler.Implicit_Attribute_Name)){
          hit = true
        } else if(hit) {
          feedback(LocatedMessage(s"All implicit bindings must be placed at the end of an Implement",param.src, Error, Checking()))
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
        feedback(LocatedMessage(s"All implicit params must be placed at the end of a $kind",param.src, Error, Checking()))
      }
    }
  }

}
