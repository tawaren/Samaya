package samaya.validation

import samaya.compilation.ErrorManager._
import samaya.structure.types.Type._
import samaya.structure.types.{Accessibility, AdtType, Capability, Func, Hash, ImplFunc, LitType, SigType, SourceId, StdFunc, Type}
import samaya.structure.{Constructor, DataDef, Field, FunctionSig, Generic, Indexed, ModuleInterface, Module, Package, Param, Result, TypeParameterized, types}
import samaya.types.Context


object SignatureValidator {

  //Checks that applies have the capabilities required by the generics
  // If promote recessives then the recursive capabilities get added to generics
  //  This is necessary when checking data type fields against the data types capabilities

  def validateTypeConstraints(t:Type, reqCaps:Set[Capability], container:TypeParameterized, context:Context, promoteGenerics:Boolean = false):Unit = {
    def checkInternal(caps:Set[Capability], generics:Seq[Generic], applies:Seq[Type]):Unit = {
      if (generics.length != applies.length) {
        feedback(LocatedMessage("Number of applied generics missmatches expected generics", t.src, Error))
      }

      for(c <- reqCaps) {
        if(!caps.contains(c)){
          feedback(LocatedMessage(s"Required Capability $c not present", t.src, Error))
        }
      }

      if(reqCaps.nonEmpty && applies.nonEmpty){
        applies.zip(generics).filter(ag => !ag._2.phantom).foreach(ag => validateTypeConstraints(ag._1, reqCaps, container, context, promoteGenerics))
      }
    }

    t match {
      case proj:Type.Projected => validateTypeConstraints(proj.inner, Set(Capability.Value), container, context, promoteGenerics)

      case adt:AdtType => adt.getEntry(context) match {
        case Some(dt) => checkInternal(dt.capabilities,dt.generics, t.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", adt.src, Error))
      }

      case lit:LitType => lit.getEntry(context) match {
        case Some(lt) => checkInternal(lt.capabilities,lt.generics, t.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", lit.src, Error))
      }

      case sig:SigType => sig.getEntry(context) match {
        case Some(st) => checkInternal(st.capabilities,st.generics, t.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", sig.src, Error))
      }

      case GenericType(capabilities, pos) =>
        if(capabilities != container.generics(pos).capabilities){
          feedback(LocatedMessage(s"Capabilities where different on type and declaration $capabilities != ${container.generics(pos).capabilities}", t.src, Error))
        }
        if(!promoteGenerics) {
          for(c <- reqCaps) {
            if(!capabilities.contains(c)){
              feedback(LocatedMessage(s"Required Capability $c not present", t.src, Error))
            }
          }
        }
      case _ =>
    }
  }

  def validateType(src:SourceId, t:Type, container:TypeParameterized, context:Context):Unit ={

    def checkInternal(generics:Seq[Generic], applies:Seq[Type]):Unit = {
      if (generics.length != applies.length) {
        feedback(LocatedMessage("Number of applied generics missmatches expected generics", src, Error))
      }

      generics.zip(applies).foreach(ag => {
        //check recursively
        validateType(ag._1.src, ag._2,container,context)
        //check constraints
        validateTypeConstraints(ag._2,ag._1.capabilities,container,context)
        //check that we never use a phantom as real
        ag._2 match {
          case GenericType(_,pos) =>
            container.generic(pos) match {
              case Some(gd) =>  if(gd.phantom && !ag._1.phantom) {
                feedback(LocatedMessage("Phantom type missmatch of generic application", ag._1.src, Error))
              }
              case None => feedback(LocatedMessage("Requested Generic does not exist", ag._1.src, Error))
            }
          case _ =>
        }
      })
    }

    t match {
      case proj:Type.Projected =>
        //todo: is this still needed?
        if(!proj.inner.hasCap(context, Capability.Value)) {
          feedback(LocatedMessage("Only value types can be projected", proj.src, Error))
        }
        validateType(proj.src, proj.inner, container, context)
      case adt:AdtType => adt.getEntry(context) match {
        case Some(dt) => checkInternal(dt.generics, t.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", adt.src, Error))
      }

      case lit:LitType => lit.getEntry(context) match {
        case Some(lt) => checkInternal(lt.generics, t.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", lit.src, Error))
      }

      case sig:SigType => sig.getEntry(context) match {
        case Some(st) => checkInternal(st.generics, t.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", sig.src, Error))
      }

      case GenericType(_,offset)=> if(container.generic(offset).isEmpty){
        feedback(LocatedMessage("Generic with requested index does not exist", t.src, Error))
      }

      case _:Type.Unknown => feedback(LocatedMessage("Requested type is unknown", t.src, Error))

    }
  }

  def validateFunction(func:Func, container:TypeParameterized, context:Context): Unit = {
    def checkInternal(generics:Seq[Generic], applies:Seq[Type]):Unit = {
      if (generics.length != applies.length) {
        feedback(LocatedMessage("Number of applied generics missmatches expected generics", func.src, Error))
      }
      generics.zip(applies).foreach(gt => SignatureValidator.validateTypeConstraints(gt._2, gt._1.capabilities, container, context))
    }

    func match {
      case std: StdFunc => std.getEntry(context) match {
        case Some(fn) => checkInternal(fn.generics, func.applies)
        case None => feedback(LocatedMessage("Requested function is not defined", std.src, Error))
      }

      case impl: ImplFunc => impl.getEntry(context) match {
        case Some(imp) => checkInternal(imp.generics, func.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", impl.src, Error))
      }

      case sig: SigType => sig.getEntry(context) match {
        case Some(sig) => checkInternal(sig.generics, func.applies)
        case None => feedback(LocatedMessage("Requested type is not defined", sig.src, Error))
      }

      case _:Func.Unknown =>
    }
  }
}
