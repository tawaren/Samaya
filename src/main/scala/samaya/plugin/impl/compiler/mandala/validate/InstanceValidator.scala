package samaya.plugin.impl.compiler.mandala.validate

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.clazz.Class
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.service.{ComponentValidator, Selectors}
import samaya.structure.types.{CompLink, Func, ImplFunc, StdFunc, Type}
import samaya.structure.types.Type.GenericType
import samaya.structure.{FunctionSig, Package}
import samaya.types.Context


object InstanceValidator {
  def validateInstance(inst: Instance, pkg: Package): Unit = {
    inst.applies.filter(_.isUnknown).foreach{ _ =>
      feedback(PlainMessage("Class apply type is unknown",Error))
    }
  }


  def validateDefInstance(inst: DefInstance, pkg: Package): Unit = {
    validateInstance(inst,pkg)
    pkg.componentByLink(inst.classTarget) match {
      case Some(cls:Class) =>
        val clsContext = Context(cls,pkg)
        if(cls.classGenerics.size != inst.applies.size){
          feedback(PlainMessage("Wrong number of class applies",Error))
        }

        for((gen,typ) <- cls.classGenerics.zip(inst.applies)){
          if(!gen.capabilities.subsetOf(typ.capabilities(clsContext))){
            feedback(PlainMessage("Class applies do not full fill capability constraints",Error))
          }
        }

        //todo: add checks for implReferences
        if(cls.functions.size + cls.implements.size != inst.funReferences.size) {
          feedback(PlainMessage("Number of instance entries does not much number of class components",Error))
        }

        for(alias <- inst.funReferences) {
          alias._2 match {
            case Instance.RemoteEntryRef(module, offset) => pkg.componentByLink(module).flatMap(_.asModule) match {
              case Some(instModule) =>
                val instContext = Context(instModule,pkg)
                cls.functions.find(_.name == alias._1) match {
                  case Some(fun) =>
                    val gens = fun.generics.drop(cls.classGenerics.size).zipWithIndex.map {
                      case (g, index) => GenericType(g.capabilities, index)
                    }
                    val instFunc = StdFunc.Remote(module, offset, gens)
                    val clsFunc = StdFunc.Remote(cls.link, fun.index, inst.applies ++ gens)
                    compareFuncs(instFunc, instContext, clsFunc, clsContext)
                    instModule.function(offset) match {
                      case Some(trgFun) => compareSignatures(inst.applies, trgFun, fun)
                      case None => feedback(PlainMessage("Cold not resolve Alias target", Error))
                    }
                  case None => feedback(PlainMessage("Alias can not be associated to a unique class function", Error))
                }
              case None => feedback(PlainMessage("Cold not resolve Alias target", Error))
            }
            case Instance.LocalEntryRef(_) => unexpected("Instances are not modules and can not have Local Links")
          }
        }
      case _ =>
        feedback(PlainMessage("Can not find class",Error))
    }
  }

  private def compareFuncs(instFunc:Func, istCtx:Context, classFunc:Func, clsCtx:Context ): Unit = {
    if(instFunc.transactional(istCtx) && !classFunc.transactional(clsCtx)) {
      feedback(PlainMessage("class component instantiation has incompatible transactional declaration",Error))
    }

    val instParams = instFunc.paramInfo(istCtx)
    val clsParams = classFunc.paramInfo(clsCtx)

    //Note Param sizes are checked in signatures as superflous params are allowed as long as they are implicit
    for(((instType,instConsume), (clsType,clsConsume)) <- instParams.zip(clsParams)){
      if(instConsume != clsConsume){
        feedback(PlainMessage("class component instantiation parameter has wrong consume attribute",Error))
      }
      if(instType != clsType) {
        feedback(PlainMessage("class component instantiation parameter has wrong type",Error))
      }
    }

    val instRets = instFunc.returnInfo(istCtx)
    val clsRets = classFunc.returnInfo(clsCtx)

    if(instRets.size != clsRets.size) {
      feedback(PlainMessage("class component instantiation has wrong number of parameters",Error))
    }

    for((instType, clsType) <- instRets.zip(clsRets)){
      if(instType != clsType) {
        feedback(PlainMessage("class component instantiation parameter has wrong type",Error))
      }
    }

  }


  private def compareSignatures(classGenerics:Seq[Type], instSig:FunctionSig, classSig:FunctionSig): Unit = {
    if(instSig.generics.size + classGenerics.size != classSig.generics.size) {
      feedback(PlainMessage("class component instantiation has mismatching amount of generics",Error))
    }

    for((instGen, clzGen) <- instSig.generics.zip(classSig.generics.drop(classGenerics.size))) {
      if(!instGen.capabilities.subsetOf(clzGen.capabilities)){
        feedback(PlainMessage("class component instantiation has stronger capability constraints than allowed",Error))
      }
    }

    if(instSig.params.size != classSig.params.size) {
      if(instSig.params.size > classSig.params.size) {
        if(!instSig.params.drop(classSig.params.size).forall(p => p.attributes.exists(a => a.name == MandalaCompiler.Implicit_Attribute_Name))){
          feedback(PlainMessage("additional class component instantiation parameter must be implicit",Error))
        }
      } else {
        feedback(PlainMessage("class component instantiation has wrong number of parameters",Error))
      }
    }
  }

}
