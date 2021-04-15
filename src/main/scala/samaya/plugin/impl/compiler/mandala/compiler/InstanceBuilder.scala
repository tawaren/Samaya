package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Compiler, Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.{MandalaCompiler, MandalaParser}
import samaya.plugin.impl.compiler.mandala.MandalaParser.InstanceContext
import samaya.plugin.impl.compiler.mandala.components.instance.{ImplInstance, Instance, MandalaDefInstanceCompilerOutput, MandalaImplInstanceCompilerOutput}
import samaya.plugin.impl.compiler.mandala.components.clazz.{FunClass, SigClass}
import samaya.plugin.impl.compiler.mandala.entry.{LocalInstanceEntry, SigImplement}
import samaya.structure.{Attribute, Binding, Component, Generic, ImplementDef, Interface, Module, Param, Result}
import samaya.structure.types.{Accessibility, AttrId, CompLink, Func, Id, ImplFunc, OpCode, Permission, SigType, SourceId, StdFunc, Type}

import scala.collection.JavaConverters._

trait InstanceBuilder extends CompilerToolbox{
  self: ComponentResolver with ComponentBuilder with CapabilityCompiler with PermissionCompiler with SigCompiler =>

  private def resolveLinks(ctx:InstanceContext):Option[(Map[String,Int], CompLink, CompLink)] = {
    val parts = ctx.compRef().path().part.asScala.map(visitName)
    val (path,targets) = resolveImport(parts, None, isEntryPath = false)
    val funLinks = targets.flatMap{
      case cls:FunClass =>
        val paramCounts = cls.functions.map(
          f => (f.name, f.params.count(
            p => !p.attributes.exists( a => a.name == MandalaCompiler.Implicit_Attribute_Name)
          ))
        ).toMap
        Some((paramCounts,cls.link))
      case _ => None
    }
    val sigLinks = targets.flatMap{
      case cls:SigClass => Some(cls.link)
      case _ => None
    }
    if(funLinks.size != 1 || sigLinks.size != 1) {
      val sourceId = sourceIdFromContext(ctx)
      if(targets.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error, Compiler()))
      } else {
        feedback(LocatedMessage(s"Class ${path.mkString(".")} does not exist", sourceId, Error, Compiler()))
      }
      None
    } else {
      val head = funLinks.head
      Some((head._1, head._2, sigLinks.head))
    }
  }

  var argCount:Map[String, Int] = Map.empty
  def withArgCount[T](argCount:Map[String, Int])(body: => T):T = {
    val oldArgCount = argCount
    this.argCount = argCount
    val res = body
    this.argCount = oldArgCount
    res
  }

  override def visitTopInstance(wrapperCtx: MandalaParser.TopInstanceContext): Unit = {
    val ctx = wrapperCtx.instanceDef().asInstanceOf[InstanceContext]
    val localGenerics = withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }

    val name = visitName(ctx.name)
    val sourceId = sourceIdFromContext(ctx)
    val mode = if(wrapperCtx.SYSTEM != null) Module.Elevated else Module.Normal
    resolveLinks(ctx) match {
      case Some((argCount, funClassLink, sigClassLink)) => withComponentBuilder(name) {
        val classApplies = withGenerics(localGenerics) {
          visitTypeRefArgs(ctx.compRef().typeRefArgs())
        }
        withComponentGenerics(localGenerics){
          val implementInfos = withArgCount(argCount) {
            ctx.instanceEntry().asScala.flatMap(visitInstanceEntry)
          }
          if (implementInfos.map(_._1).distinct.size != implementInfos.size) {
            feedback(LocatedMessage("Alias defined multiple times", sourceId, Error, Compiler()))
          }
          val (impl, implMapping) = implInstance(name, localGenerics, sigClassLink, mode, classApplies, implementInfos, sourceId)

          build(impl, sourceId) match {
            case Some(implBuild) =>
              val implements = implementInfos.flatMap{
                case (name,gen,fun,src) => implMapping.get(name).map{ index =>
                  SigImplement(name,gen,fun, ImplFunc.Remote(implBuild.link, index, gen.map(_.asType(src)))(src), src)
                }
              }
              val inst = defInstance(
                name,
                localGenerics,
                funClassLink,
                classApplies,
                implements,
                sourceId
              )
              build(inst,sourceId)
            case None =>
          }
        }
      }
      case None =>
    }

  }

  override def visitInstance(ctx: MandalaParser.InstanceContext):Unit = {
    val sourceId = sourceIdFromContext(ctx)
    val instName = visitName(ctx.name())
    val localGenerics = withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }
    resolveLinks(ctx) match {
      case Some((argCount, clazzLink, sigLink)) =>
        val classApplies = withGenerics(localGenerics) {
          visitTypeRefArgs(ctx.compRef().typeRefArgs())
        }
        withComponentGenerics(localGenerics){
          val implementInfos = withArgCount(argCount) {
            ctx.instanceEntry().asScala.flatMap(visitInstanceEntry)
          }
          if (implementInfos.map(_._1).distinct.size != implementInfos.size) {
            feedback(LocatedMessage("Alias defined multiple times", sourceId, Error, Compiler()))
          }

          val implMapping:Map[String,Int] = env.pkg.componentByLink(sigLink) match {
            case Some(targComp:SigClass) =>
              implementInfos.flatMap {
                case (implName, implGenerics, target, sourceId) => generateImplement(instName, classApplies, implName, implGenerics, target, targComp, sourceId).map(impl => (implName,impl.index))
              }.toMap
            case _ =>
              //todo: can this happen or is this an unexpected
              //  Try to provoke
              feedback(LocatedMessage("Can not resolve implemented signature",sourceId,Error, Compiler()))
              Map.empty
          }

          val implements = implementInfos.flatMap{
            case (name,gen,fun,src) => implMapping.get(name).map{ index =>
              SigImplement(name,gen,fun, ImplFunc.Local(index, gen.map(_.asType(src)))(src), src)
            }
          }
          registerInstanceEntry(LocalInstanceEntry(instName,localGenerics, clazzLink, implements, classApplies, sourceId))
        }
      case None =>
    }

  }

  override def visitInstanceEntry(ctx: MandalaParser.InstanceEntryContext): Option[(String, Seq[Generic], StdFunc, SourceId)] = {
    withFreshCounters {
      super.visitInstanceEntry(ctx).asInstanceOf[Option[(String, Seq[Generic], StdFunc, SourceId)]]
    }
  }

  override def visitAliasDef(ctx: MandalaParser.AliasDefContext): Option[(String, Seq[Generic], StdFunc, SourceId)] = {
    val name = visitName(ctx.name())
    val localGenerics = withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }
    val target = withGenerics(localGenerics) {
      visitFunRef(ctx.baseRef(), argCount.get(name))
    }
    target.asStdFunc.map((name, localGenerics, _, sourceIdFromContext(ctx)))
  }

  def defInstance(name:String, instanceGenerics:Seq[Generic], classTarget:CompLink, classApplies:Seq[Type], sigImplements:Seq[SigImplement], sourceId: SourceId): Instance = {
    val res = new MandalaDefInstanceCompilerOutput(
      name,
      instanceGenerics,
      classTarget,
      classApplies,
      sigImplements,
      sourceId,
    )
    env.instRec(res, isLocal = false)
    res
  }

  private def implInstance(instName:String, generics:Seq[Generic], sigTarget:CompLink, mode:Module.Mode, classApplies:Seq[Type], implementInfos:Seq[(String, Seq[Generic], Func, SourceId)], srcId:SourceId): (ImplInstance, Map[String,Int]) = {
    withComponentBuilder(instName){
      val mapping:Map[String,Int] = env.pkg.componentByLink(sigTarget) match {
        case Some(targComp:SigClass) =>
          implementInfos.flatMap {
            case (implName, implGenerics, target, sourceId) => generateImplement(instName, classApplies, implName, implGenerics, target, targComp, sourceId).map(impl => (implName,impl.index))
          }.toMap
        case _ =>
          //todo: can this happen or is this an unexpected
          //  Try to provoke
          feedback(LocatedMessage("Can not resolve implemented signature",srcId,Error, Compiler()))
          Map.empty
      }
      (new MandalaImplInstanceCompilerOutput(
        instName,
        mode,
        generics,
        sigTarget,
        classApplies,
        currentComponent._implements,
        srcId
      ), mapping)
    }

  }

  def generateImplement(instName:String, classApplies:Seq[Type], implName:String, implGenerics:Seq[Generic], targetFun:Func, sigTarget:Interface[Component] with SigClass, srcId:SourceId): Option[ImplementDef] = {
    (targetFun.asStdFunc.flatMap(_.getEntry(context)),sigTarget.signatures.find(f => f.name == implName)) match {
      case (Some(implFun), Some(implSig)) =>
        val implParams = implFun.params.drop(implSig.params.size)
        val genTypes = implGenerics.drop(componentGenerics.size).map(_.asType(srcId))
        Some(registerImplementDef(new ImplementDef {
          override val name: String = Instance.deriveTopName(instName,implName)
          override val index: Int = nextImplIndex()
          override val position: Int = nextPosition()
          override val external: Boolean = false
          override val attributes: Seq[Attribute] = Seq.empty
          override val generics: Seq[Generic] = implGenerics
          override val accessibility: Map[Permission, Accessibility] = Map(Permission.Call -> Accessibility.Global)
          override val transactional: Boolean = implSig.transactional

          override val sigParamBindings: Seq[Binding] = implSig.params.map(p => new Binding {
            override val name: String = p.name
            override val index: Int = p.index
            override val attributes: Seq[Attribute] = p.attributes
            override val src: SourceId = srcId
          })

          override val sigResultBindings: Seq[Binding] = implSig.results.map(r => new Binding {
            override val name: String = r.name
            override val index: Int = r.index
            override val attributes: Seq[Attribute] = r.attributes
            override val src: SourceId = srcId
          })

          override val params: Seq[Param] = withFreshIndex(implParams.map{ p => new Param {
            override val name: String = p.name
            override val index: Int = nextIndex()
            override val typ: Type = p.typ
            override val consumes: Boolean = true
            override val attributes: Seq[Attribute] = p.attributes
            override val src: SourceId = srcId
          }
          })

          override val results: Seq[Result] = Seq(new Result {
            override val name: String = implName
            override val index: Int = 0
            override val typ: Type = SigType.Remote(sigTarget.link, implSig.index, classApplies ++ genTypes)(srcId)
            override val attributes: Seq[Attribute] = Seq.empty
            override val src: SourceId = srcId
          })

          override val code: Seq[OpCode] = {
            val res = implSig.results.map(r => AttrId(Id(r.name, r.src), Seq.empty))
            val params = implSig.params.map(p => Id(p.name, p.src)) ++ implParams.map(r => Id(r.name, r.src))
            Seq(OpCode.Invoke(res, targetFun, params, srcId))
          }
          override val src:SourceId = srcId
        }))
      case _ =>
        //todo: can this happen or is this an unexpected
        //  Try to provoke
        feedback(LocatedMessage("Can not resolve implement target",srcId,Error, Compiler()))
        None
    }
  }
}
