package samaya.plugin.impl.compiler.mandala.process


import samaya.compilation.ErrorManager.{Compiler, Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.instance.DefInstance
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.mandala.components.clazz.Class
import samaya.structure.{Attribute, Package}
import samaya.structure.types.Type.RemoteLookup
import samaya.structure.types.{CompLink, Func, SourceId, Type}
import samaya.toolbox.process.TypeInference
import samaya.types.Context
import scala.annotation.tailrec

class InstanceFinder(imported:Map[CompLink,Seq[InstInfo]], localized:Map[CompLink,Seq[InstInfo]]) {

  private def sequentialMatches(matches:Map[Int,Type], gens:Int, src:SourceId):Seq[Type] = {
    var res = Seq.empty[Type]
    for(i <- 0 until gens){
      if(matches.contains(i)){
        res = res :+ matches(i)
      } else {
        res = res :+ TypeInference.TypeVar(src)
      }
    }
    res
  }

  private def matchAndExtract(inst:InstInfo, clazzApplies:Seq[Type], src:SourceId):Option[Seq[Type]] = {
    Type.matchGenerics(inst.classApplies,clazzApplies).map(sequentialMatches(_, inst.generics.size, src))
  }

  private def findInLocal(clazz:CompLink, clazzApplies:Seq[Type], src:SourceId): Seq[(Seq[Type],InstInfo)] = {
    localized.get(clazz) match {
      case Some(value) => value.flatMap(inst => matchAndExtract(inst,clazzApplies, src).map(m => (m, inst)))
      case None => Seq.empty
    }
  }

  private def findImported(clazz:CompLink, clazzApplies:Seq[Type], src:SourceId): Seq[(Seq[Type],InstInfo)] = {
    imported.get(clazz) match {
      case Some(value) => value.flatMap(inst => matchAndExtract(inst,clazzApplies, src).map(m => (m, inst)))
      case None =>Seq.empty
    }
  }

  //This gets confusing when used together with aliases
  // Because it does not find Instances defined in the same Module as the Alias
  // It only finds them in the target od the alias
  //  Can we get it somehow to work with aliases without introducing them to samaya?T
  private def findInApplies(clazz:CompLink, clazzApplies:Seq[Type], ctx:Context, src:SourceId): Seq[(Seq[Type],InstInfo)] = {
    def resolveCompanionInstancesByModule(containingPkg:Package, mod:MandalaModule): Seq[(Seq[Type],InstInfo)] = {
      mod.instances.get(clazz) match {
        case Some(candidates) => candidates
          .flatMap(containingPkg.componentByName)
          .filter(_.isInstanceOf[DefInstance])
          .map(_.asInstanceOf[DefInstance])
          .flatMap(inst => matchAndExtract(inst,clazzApplies, src).map(m => (m, inst)))
        case None => Seq.empty
      }
    }

    @tailrec
    def resolveCompanionInstancesByType(typ:Type): Seq[(Seq[Type],InstInfo)] = {
      typ match {
        case project: Type.Projected => resolveCompanionInstancesByType(project.inner)
        case definedType: RemoteLookup[_] => (definedType.getPackage(ctx), definedType.getComponent(ctx)) match {
          case (Some(containingPkg),Some(mod:MandalaModule)) => resolveCompanionInstancesByModule(containingPkg,mod)
          case _ => Seq.empty
        }
        case _ => Seq.empty
      }
    }

    clazzApplies.flatMap{ typ =>
      resolveCompanionInstancesByType(typ) ++ typ.attributes.filter(p => p.name == MandalaCompiler.Aliasing_Module_Attribute_Name).flatMap{
        case Attribute(_, Attribute.Text(modLink)) =>
          val link = CompLink.fromString(modLink)
          (ctx.pkg.packageOfLink(link),ctx.pkg.componentByLink(link)) match {
            case (Some(containingPkg),Some(mod:MandalaModule)) => resolveCompanionInstancesByModule(containingPkg,mod)
            case _ => Seq.empty
          }
        case _ => Seq.empty
      }
    }
  }

  def findInstances(clazz:CompLink, clazzApplies:Seq[Type], ctx:Context, src:SourceId):Seq[(Seq[Type],InstInfo)] = {
    val imported = findImported(clazz, clazzApplies, src)
    val local = findInLocal(clazz, clazzApplies, src)
    val companion = findInApplies(clazz, clazzApplies, ctx, src)
    imported ++ local ++ companion
  }

  def findAndApply(clazz:CompLink, clazzApplies:Seq[Type], functionApplies:Seq[Type], ctx:Context, src:SourceId)(f: InstInfo => Option[Func]):Option[Func] = {
    val candidates = findInstances(clazz, clazzApplies, ctx, src).flatMap(mi =>  f(mi._2).map((mi._1,_))).toSet
    val baseApplies = functionApplies.drop(clazzApplies.size)
    if(candidates.size == 1){
      Some(candidates.head match {
        case (matches, func) =>
          val substitution = matches ++ baseApplies
          def replaceFun(t:Type):Type = t match {
            case Type.GenericType(_, index) => substitution(index)
            case typ => typ.replaceContainedTypes(replaceFun)
          }
          func.replaceContainedTypes(replaceFun)
      })
    } else {
      val pkgName = ctx.pkg.packageOfLink(clazz).map(_.name+".").getOrElse("")
      val cls = ctx.pkg.componentByLink(clazz) match {
        case Some(cls:Class) => s"$pkgName${cls.name}[${clazzApplies.map(_.prettyString(ctx)).mkString(",")}]"
        case None => "unknown"
      }
      if(candidates.isEmpty) {
        feedback(LocatedMessage(s"Instance resolution for class $cls failed",src,Error, Compiler()))
      } else {
        val pretty = candidates.map{
          case (matches, func) =>
            //todo: we hav recursive pretty print -- how can we make better
            val substitution = (matches ++ baseApplies).map(_.prettyString(ctx))
            func.prettyString(ctx, substitution)
        }
        feedback(LocatedMessage(s"Instance resolution for class $cls produced multiple candidates: ${pretty.mkString(",")}",src,Error, Compiler()))
      }
      //We have an ambiguity
      None
    }
  }

  def findAndApplyTargetFunction(name:String, clazz:CompLink, clazzApplies:Seq[Type], functionApplies:Seq[Type], ctx:Context, src:SourceId):Option[Func] = {
    findAndApply(clazz,clazzApplies,functionApplies,ctx,src)(_.implements.find(_.name == name).map(_.funTarget))
  }

  def findAndApplyImplementFunction(name:String, clazz:CompLink, clazzApplies:Seq[Type], functionApplies:Seq[Type], ctx:Context, src:SourceId):Option[Func] = {
    findAndApply(clazz,clazzApplies,functionApplies,ctx,src)(_.implements.find(_.name == name).map(_.implTarget))
  }
}
