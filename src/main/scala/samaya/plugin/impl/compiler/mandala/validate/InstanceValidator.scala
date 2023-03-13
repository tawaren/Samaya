package samaya.plugin.impl.compiler.mandala.validate

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.clazz.{Class, SigClass}
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.structure.types.{Func, SigType, SourceId, StdFunc}
import samaya.structure.{FunctionSig, Generic, Package}
import samaya.types.Context
import samaya.validation.SignatureValidator

object InstanceValidator {
  def validateInstance(inst: Instance, pkg: Package): Unit = {
    inst.classApplies.filter(_.isUnknown).foreach{ ca =>
      feedback(LocatedMessage("Class apply type is unknown", ca.src, Error, Checking()))
    }
  }


  def validateDefInstance(inst: DefInstance, pkg: Package): Unit = {
    validateInstance(inst,pkg)
    val defContext = Context(pkg)
    pkg.componentByLink(inst.classTarget) match {
      case Some(cls:Class) =>
        val clsContext = Context(cls,pkg)
        if(cls.generics.size != inst.classApplies.size){
          feedback(LocatedMessage("Wrong number of class applies",inst.src,Error, Checking()))
        }

        for((gen,typ) <- cls.generics.zip(inst.classApplies)){
          if(!gen.capabilities.subsetOf(typ.capabilities(clsContext))){
            feedback(LocatedMessage("Class applies do not full fill capability constraints",typ.src,Error, Checking()))
          }
        }

        if(cls.functions.size != inst.implements.size) {
          feedback(LocatedMessage("Number of instance entries does not match number of class components", inst.src, Error, Checking()))
        }
        val names = inst.implements.map(_.name)
        if(names.size != names.distinct.size) {
          feedback(LocatedMessage("Instance implementations must have unique names", inst.src, Error, Checking()))
        }

        for(si@SigImplement(name, generics, funTarget, implTarget, src) <- inst.implements) {
          SignatureValidator.validateFunction(funTarget, si, defContext)
          SignatureValidator.validateFunction(implTarget, si, defContext)

          if(generics.take(inst.generics.size) != inst.generics) {
            feedback(LocatedMessage("Generics on implementation must start with the same generics as the instance", src, Error, Checking()))
          }

          //This is ok for now, as an instance never has locals
          val plainContext = Context(pkg)

          val implRets = implTarget.returnInfo(plainContext)
          if(implRets.size != 1){
            feedback(LocatedMessage("Instance implements must return a single signature", implTarget.src, Error, Checking()))
          } else {
            implRets.head match {
              case sig:SigType =>
                sig.getEntry(plainContext) match {
                  case Some(sigDef) if sigDef.name == name  =>
                  case _ => feedback(LocatedMessage("Instance implements must return a signature with the same name aas the implement",sig.src,Error, Checking()))
                }
                sig.getComponent(plainContext) match {
                  case Some(sigCls:SigClass) => if(sigCls.clazzLink != inst.classTarget){
                    feedback(LocatedMessage("Instance implements must return a signature from the implemented class",sig.src,Error, Checking()))
                  }
                  case _ => feedback(LocatedMessage("Instance implements must return a signature from a Signature class",sig.src,Error, Checking()))
                }
              case _ => feedback(LocatedMessage("Instance implements must return a single signature",implTarget.src, Error, Checking()))
            }
          }

          cls.functions.find(_.name == name) match {
            case None =>
            case Some(fun) =>
              val genTypes = generics.drop(inst.generics.size).map(_.asType(fun.src))
              val clsFunc = StdFunc.Remote(cls.link, fun.index, inst.classApplies ++ genTypes)(fun.src)
              compareFuncs(funTarget, plainContext, clsFunc, clsContext)
              compareGenerics(inst.generics.size, generics, cls.generics.size, fun.generics, src)
              funTarget.getEntry(plainContext) match {
                case Some(trgFun) => checkParamSizes(trgFun, fun, src)
                case None => feedback(LocatedMessage("Cold not resolve target definition", src, Error, Checking()))
              }
          }
        }
      case _ =>
        feedback(LocatedMessage("Can not find class", inst.src, Error, Checking()))
    }
  }

  private def compareFuncs(instFunc:Func, istCtx:Context, classFunc:Func, clsCtx:Context ): Unit = {
    if(instFunc.transactional(istCtx) && !classFunc.transactional(clsCtx)) {
      feedback(LocatedMessage("class component instantiation has incompatible transactional declaration",instFunc.src,Error, Checking()))
    }

    val instParams = instFunc.paramInfo(istCtx)
    val clsParams = classFunc.paramInfo(clsCtx)

    //Note Param sizes are checked in signatures as superflous params are allowed as long as they are implicit
    for(((instType,instConsume), (clsType,clsConsume)) <- instParams.zip(clsParams)){
      if(instConsume != clsConsume){
        feedback(LocatedMessage("class component instantiation parameter has wrong consume attribute",instType.src,Error, Checking()))
      }
      if(instType != clsType) {
        feedback(LocatedMessage("class component instantiation parameter has wrong type",instType.src,Error, Checking()))
      }
    }

    val instRets = instFunc.returnInfo(istCtx)
    val clsRets = classFunc.returnInfo(clsCtx)

    if(instRets.size != clsRets.size) {
      feedback(LocatedMessage("class component instantiation has wrong number of parameters",instFunc.src,Error, Checking()))
    }

    for((instType, clsType) <- instRets.zip(clsRets)){
      if(instType != clsType) {
        feedback(LocatedMessage("class component instantiation parameter has wrong type", instType.src, Error, Checking()))
      }
    }
  }

  private def compareGenerics(numInstGenerics:Int, implGenerics:Seq[Generic], numClassGenerics:Int, clsFunGenerics:Seq[Generic], src:SourceId): Unit = {
    val exclusiveImplGenerics = implGenerics.drop(numInstGenerics)
    val exclusiveClassFunGenerics = clsFunGenerics.drop(numClassGenerics)

    if(exclusiveImplGenerics.size != exclusiveClassFunGenerics.size) {
      feedback(LocatedMessage("class component instantiation has mismatching amount of generics",src,Error, Checking()))
    }

    for((instGen, clzGen) <- exclusiveImplGenerics.zip(exclusiveClassFunGenerics)) {
      if(!instGen.capabilities.subsetOf(clzGen.capabilities)){
        feedback(LocatedMessage("class component instantiation has stronger capability constraints than allowed",instGen.src,Error, Checking()))
      }
    }
  }

  private def checkParamSizes(instSig:FunctionSig, classSig:FunctionSig, src:SourceId): Unit = {
    if(instSig.params.size != classSig.params.size) {
      if(instSig.params.size > classSig.params.size) {
        if(!instSig.params.drop(classSig.params.size).forall(p => p.attributes.exists(a => a.name == MandalaCompiler.Implicit_Attribute_Name))){
          feedback(LocatedMessage("additional class component instantiation parameter must be implicit",src,Error, Checking()))
        }
      } else {
        feedback(LocatedMessage("class component instantiation has wrong number of parameters",src,Error, Checking()))
      }
    }
  }

}
