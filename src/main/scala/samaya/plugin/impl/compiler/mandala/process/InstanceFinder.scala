package samaya.plugin.impl.compiler.mandala.process

import samaya.compilation.ErrorManager.{Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.instance.DefInstance
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.{LocalEntryRef, RemoteEntryRef}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.structure.types.Type.RemoteLookup
import samaya.structure.types.{CompLink, DefinedType, Func, ImplFunc, SourceId, StdFunc, Type}
import samaya.types.Context

class InstanceFinder(imported:Map[CompLink,Seq[InstInfo]], localized:Map[CompLink,Seq[InstInfo]]) {

  private def findInLocal(clazz:CompLink, clazzApplies:Seq[Type]): Seq[InstInfo] = {
    localized.get(clazz) match {
      case Some(value) => value.filter(inst => inst.applies == clazzApplies)
      case None =>Seq.empty
    }
  }

  private def findImported(clazz:CompLink, clazzApplies:Seq[Type]): Seq[InstInfo] = {
    imported.get(clazz) match {
      case Some(value) => value.filter(inst => inst.applies == clazzApplies)
      case None =>Seq.empty
    }
  }

  private def findInApplies(clazz:CompLink, clazzApplies:Seq[Type], ctx:Context): Seq[InstInfo] = {
    clazzApplies.flatMap{
      //Note: Locals are found over findInLocal & Unknowns, Generics have no companion anyway
      case definedType: RemoteLookup[_] => definedType.getComponent(ctx) match {
        case Some(mod:MandalaModule) =>
          mod.instances.get(clazz) match {
            case Some(candidates) => definedType.getPackage(ctx) match {
                case Some(containingPkg) => candidates
                    .flatMap(containingPkg.componentByName)
                    .filter(_.isInstanceOf[DefInstance])
                    .map(_.asInstanceOf[DefInstance])
                    .filter(_.applies == clazzApplies)
                case None => Seq.empty
              }
            case None => Seq.empty
          }
        case _ => Seq.empty
      }
      case _ => Seq.empty
    }
  }

  def findInstances(clazz:CompLink, clazzApplies:Seq[Type], ctx:Context):Seq[InstInfo] = {
    val imported = findImported(clazz, clazzApplies)
    val local = findInLocal(clazz, clazzApplies)
    val companion = findInApplies(clazz, clazzApplies, ctx)
    imported ++ local ++ companion
  }

  def findAndApplyTargetFunction(name:String, clazz:CompLink, clazzApplies:Seq[Type], functionApplies:Seq[Type], ctx:Context, src:SourceId):Option[Func] = {
    val candidates =  findInstances(clazz, clazzApplies, ctx).flatMap(inst => inst.funReferences.get(name)).toSet
    if(candidates.size == 1){
      Some(candidates.head match {
        case RemoteEntryRef(module, offset) => StdFunc.Remote(module, offset, functionApplies.drop(clazzApplies.size))
        //Only place where this is allowed as it may come from findInLocal which accesses localized
        case LocalEntryRef(offset) => StdFunc.Local(offset, functionApplies.drop(clazzApplies.size))
      })
    } else {
      //We have an ambiguity
      feedback(LocatedMessage(s"${candidates.size} instances where available but 1 was expected",src,Error))
      None
    }
  }

  def findAndApplyImplementFunction(name:String, clazz:CompLink, clazzApplies:Seq[Type], functionApplies:Seq[Type], ctx:Context, src:SourceId):Option[Func] = {
    val candidates =  findInstances(clazz, clazzApplies, ctx).flatMap(inst => inst.implReferences.get(name)).toSet
    if(candidates.size == 1){
      Some(candidates.head match {
        case RemoteEntryRef(module, offset) => ImplFunc.Remote(module, offset, functionApplies.drop(clazzApplies.size))
        //Only place where this is allowed as it may come from findInLocal which accesses localized
        case LocalEntryRef(offset) => ImplFunc.Local(offset, functionApplies.drop(clazzApplies.size))
      })
    } else {
      //We have an ambiguity
      feedback(LocatedMessage(s"${candidates.size} instances where available but 1 was expected",src,Error))
      None
    }
  }
}
