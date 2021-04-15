package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Compiler, LocatedMessage, Warning, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.structure.types.Accessibility.{Global, Guarded, Local}
import samaya.structure.types.Permission.{Call, Consume, Create, Define, Inspect}
import samaya.structure.types.{Accessibility, Permission}

import scala.collection.JavaConverters._

trait PermissionCompiler extends CompilerToolbox {
  private var defaultAccess:Accessibility = Local

  def withDefaultAccess[T](accessDefault:Accessibility)(body: => T):T = {
    val oldDefaultAccess = defaultAccess
    defaultAccess = accessDefault
    val res = body
    defaultAccess = oldDefaultAccess
    res
  }

  private var supportedPerms:Set[Permission] = Set.empty
  def withSupportedPerms[T](permsSupported:Set[Permission])(body: => T):T = {
    val oldSupportedPerms = supportedPerms
    supportedPerms = permsSupported
    val res = body
    supportedPerms = oldSupportedPerms
    res
  }

  private var accessibilities:Map[Permission, Accessibility] = Map.empty[Permission, Accessibility]
  override def visitAccessibilities(ctx: MandalaParser.AccessibilitiesContext):Map[Permission,Accessibility]  = {
    accessibilities = Map.empty[Permission, Accessibility]
    super.visitAccessibilities(ctx)
    //Remove unsupported Keys
    //filter Map seems to be broken it somehow creates a Map that for now obvious reason clears it self even as it should be immutable
    // detected as SystemHash/Referenz stays the same but content changes (empties) which violates the immutable property
    //val result:Map[Permission,Accessibility] = accessibilities.filterKeys(p => supportedPerms.contains(p))
    val result = accessibilities.foldLeft(Map.empty[Permission,Accessibility]){
      case (r,(k,v) ) => if(supportedPerms.contains(k)) {
        r.updated(k,v)
      } else {
        r
      }
    }

    if(accessibilities.size != result.size) {
      feedback(LocatedMessage(s"Unsupported Accessibility is ignored", sourceIdFromContext(ctx), Warning, Compiler()))
    }

    //Add defaults for unspecified Keys
    supportedPerms.foldLeft(result){ case (res,p) => {
      if(!res.contains(p)) {
        res.updated(p, defaultAccess)
      } else {
        res
      }
    }}
  }

  private def addAccessibility(perms:Set[Permission], access:Accessibility): Unit = {
    val delaultedPerms = if(perms.isEmpty) {
      supportedPerms
    } else {
      perms
    }
    delaultedPerms.foreach(p => {
      accessibilities = accessibilities.updated(p,access)
    })
  }

  override def visitGlobal(ctx: MandalaParser.GlobalContext): Unit = addAccessibility(ctx.p.asScala.map(visitPermission).toSet, Global)
  override def visitLocal(ctx: MandalaParser.LocalContext): Unit  = addAccessibility(ctx.p.asScala.map(visitPermission).toSet, Local)
  override def visitGuarded(ctx: MandalaParser.GuardedContext): Unit  = addAccessibility(ctx.p.asScala.map(visitPermission).toSet, Guarded(ctx.g.asScala.map(visitName).toSet))

  override def visitPermission(ctx: MandalaParser.PermissionContext): Permission = {
    if(ctx.CREATE() != null) return Create
    if(ctx.INSPECT() != null) return Inspect
    if(ctx.CONSUME() != null) return Consume
    if(ctx.CALL() != null) return Call
    if(ctx.DEFINE() != null) return Define
    null
  }

}
