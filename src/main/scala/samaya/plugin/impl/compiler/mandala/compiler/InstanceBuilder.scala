package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Error, LocatedMessage, feedback, unexpected}
import samaya.plugin.impl.compiler.mandala.components.instance.{ImplInstance, Instance, MandalaDefInstanceCompilerOutput, MandalaImplInstanceCompilerOutput}
import samaya.plugin.impl.compiler.mandala.components.clazz.{FunClass, SigClass}
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.{EntryRef, LocalEntryRef, RemoteEntryRef}
import samaya.plugin.impl.compiler.mandala.entry.instance.LocalInstanceEntry
import samaya.plugin.impl.compiler.simple.MandalaParser
import samaya.plugin.impl.compiler.simple.MandalaParser.InstanceContext
import samaya.structure.{Attribute, Module, Binding, Component, Generic, ImplementDef, Interface, Param, Result}
import samaya.structure.types.{Accessibility, AttrId, Capability, CompLink, Id, OpCode, Permission, SigType, SourceId, StdFunc, Type}

import scala.collection.JavaConverters._

trait InstanceBuilder extends CompilerToolbox{
  self: ComponentResolver with ComponentBuilder with CapabilityCompiler with PermissionCompiler with SigCompiler =>

  private def resolveLinks(ctx:InstanceContext):Option[(CompLink, CompLink)] = {
    val parts = ctx.baseRef().path().part.asScala.map(visitName)
    val (path,targets) = resolveImport(parts, isEntryPath = false)
    val funLinks = targets.flatMap{
      case cls:FunClass => Some(cls.link)
      case _ => None
    }
    val sigLinks = targets.flatMap{
      case cls:SigClass => Some(cls.link)
      case _ => None
    }
    if(funLinks.size != 1 || sigLinks.size != 1) {
      val sourceId = sourceIdFromContext(ctx)
      if(targets.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error))
      } else {
        feedback(LocatedMessage(s"Class ${path.mkString(".")} does not exist", sourceId, Error))
      }
      None
    } else {
      Some(funLinks.head, sigLinks.head)
    }
  }

  override def visitTopInstance(wrapperCtx: MandalaParser.TopInstanceContext): Unit = {
    val ctx = wrapperCtx.instanceDef().asInstanceOf[InstanceContext]
    val name = visitName(ctx.name)
    val sourceId = sourceIdFromContext(ctx)
    val mode = if(wrapperCtx.SYSTEM != null) Module.Elevated else Module.Normal
    resolveLinks(ctx) match {
      case Some((funLink, sigLink)) => withComponentBuilder(name) {
        val applies = visitTypeRefArgs(ctx.baseRef().typeRefArgs())
        val funAliases = ctx.instanceEntry().asScala.flatMap(visitInstanceEntry)
        if (funAliases.map(_._1).distinct.size != funAliases.size) {
          feedback(LocatedMessage("Alias defined multiple times", sourceId, Error))
        }
        val (impl, implMapping) = implInstance(name, sigLink, mode, applies, funAliases, sourceId)

        build(impl, sourceId) match {
          case Some(implBuild) =>
            val implAliases = funAliases.flatMap{
              case (name, _, _) => implMapping.get(name).map{ index =>
                (name, RemoteEntryRef(implBuild.link, index):EntryRef)
              }
            }
            val inst = defInstance(
              name,
              funLink,
              applies,
              funAliases.map(e => (e._1, e._2)).toMap,
              implAliases.toMap
            )
            build(inst,sourceId)
          case None =>
        }
      }
      case None =>
    }
  }

  override def visitInstance(ctx: MandalaParser.InstanceContext):Unit = {
    val sourceId = sourceIdFromContext(ctx)
    val name = visitName(ctx.name())
    resolveLinks(ctx) match {
      case Some((clazzLink, sigLink)) =>
        val applies = visitTypeRefArgs(ctx.baseRef().typeRefArgs())
        val funAliases = ctx.instanceEntry().asScala.flatMap(visitInstanceEntry)
        if (funAliases.map(_._1).distinct.size != funAliases.size) {
          feedback(LocatedMessage("Alias defined multiple times", sourceId, Error))
        }

        val implMapping:Map[String,Int] = env.pkg.componentByLink(sigLink) match {
          case Some(targComp:SigClass) =>
            funAliases.flatMap {
              case (implName, compRef, sourceId) => generateImplement(name, implName,targComp, applies, compRef, sourceId).map(impl => (implName,impl.index))
            }.toMap
          case _ =>
            //todo: can this happen or is this an unexpected
            //  Try to provoke
            feedback(LocatedMessage("Can not resolve implemented signature",sourceId,Error))
            Map.empty
        }

        val funRefs = funAliases.map(e => (e._1, e._2)).toMap
        val implRefs = funRefs.flatMap{
          case (name, _) => implMapping.get(name).map{ index =>
            (name, LocalEntryRef(index):EntryRef)
          }
        }

        registerInstanceEntry(LocalInstanceEntry(name,clazzLink, funRefs, implRefs, applies, sourceId))
      case None =>
    }

  }

  override def visitInstanceEntry(ctx: MandalaParser.InstanceEntryContext): Option[(String, Instance.EntryRef, SourceId)] = {
    withFreshCounters {
      super.visitInstanceEntry(ctx).asInstanceOf[Option[(String,Instance.EntryRef, SourceId)]]
    }
  }

  override def visitAliasDef(ctx: MandalaParser.AliasDefContext): Option[(String, Instance.EntryRef, SourceId)] = {
    val name = visitName(ctx.name())
    val elems = ctx.path().part.asScala.map(visitName)
    val src = sourceIdFromContext(ctx)
    resolveAliasTarget(elems, src).map((name, _, src))
  }

  def defInstance(name:String, classTarget:CompLink, applies:Seq[Type], funAliases:Map[String, Instance.EntryRef], implAliases:Map[String, Instance.EntryRef]): Instance = {
    val res = new MandalaDefInstanceCompilerOutput(
      name,
      classTarget,
      applies,
      funAliases,
      implAliases,
    )
    env.instRec(res, isLocal = false)
    res
  }

  private def implInstance(name:String, sigTarget:CompLink, mode:Module.Mode, applies:Seq[Type], funAliases:Seq[(String, Instance.EntryRef, SourceId)], srcId:SourceId): (ImplInstance, Map[String,Int]) = {
    withComponentBuilder(name){
      val mapping:Map[String,Int] = env.pkg.componentByLink(sigTarget) match {
        case Some(targComp:SigClass) =>
          funAliases.flatMap {
            case (implName, compRef, sourceId) => generateImplement(name, implName,targComp, applies, compRef, sourceId).map(impl => (implName,impl.index))
          }.toMap
        case _ =>
          //todo: can this happen or is this an unexpected
          //  Try to provoke
          feedback(LocatedMessage("Can not resolve implemented signature",srcId,Error))
          Map.empty
      }
      (new MandalaImplInstanceCompilerOutput(
        name,
        mode,
        sigTarget,
        applies,
        currentComponent._implements
      ), mapping)
    }

  }

  def generateImplement(instName:String, implName:String, sigTarget:Interface[Component] with SigClass, applies:Seq[Type], compRef:Instance.EntryRef, srcId:SourceId): Option[ImplementDef] = {
    val implFunRes = compRef match {
      case RemoteEntryRef(module, offset) => env.pkg.componentByLink(module).flatMap(_.asModule).map(cmp => cmp.functions(offset))
      case LocalEntryRef(offset) => context.module.map(cmp => cmp.functions(offset))
    }

    val implSigRes = sigTarget.signatures.find(f => f.name == implName)
    (implFunRes, implSigRes) match {
      case (Some(implFun), Some(implSig)) =>

        val cleanedGenerics = withFreshIndex(implSig.generics.drop(applies.size).map(g => new Generic {
          override val name: String = g.name
          override val index: Int = nextIndex()
          override val phantom: Boolean = g.phantom
          override val capabilities: Set[Capability] = g.capabilities
          override val attributes: Seq[Attribute] = g.attributes
          override val src: SourceId = srcId
        }))

        val genTypes = cleanedGenerics.map{g => Type.GenericType (g.capabilities,g.index)}
        Some(registerImplementDef(new ImplementDef {
          override val name: String = Instance.deriveTopName(instName,implName)
          override val index: Int = nextImplIndex()
          override val position: Int = nextPosition()
          override val external: Boolean = false
          override val attributes: Seq[Attribute] = Seq.empty
          override val generics: Seq[Generic] = cleanedGenerics
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

          override val params: Seq[Param] = withFreshIndex(implFun.params.drop(implSig.params.size).map{ p => new Param {
            override val name: String = p.name
            override val index: Int = nextIndex()
            override val typ: Type = p.typ
            override val consumes: Boolean = p.consumes
            override val attributes: Seq[Attribute] = p.attributes
            override val src: SourceId = srcId
          }
          })

          override val results: Seq[Result] = Seq(new Result {
            override val name: String = implName
            override val index: Int = 0
            override val typ: Type = SigType.Remote(sigTarget.link, implSig.index, applies ++ genTypes)
            override val attributes: Seq[Attribute] = Seq.empty
            override val src: SourceId = srcId
          })
          override val code: Seq[OpCode] = {
            val res = implSig.results.map(r => AttrId(Id(r.name), Seq.empty))
            val implParam = implFun.params.drop(implSig.params.size).map(r => Id(r.name))
            val sigParams = implSig.params.map(p => Id(p.name))
            val fun = compRef match {
              case RemoteEntryRef(module, offset) => StdFunc.Remote(module, offset, genTypes)
              case LocalEntryRef(offset) => StdFunc.Local(offset, genTypes)
            }
            val params = sigParams ++ implParam
            Seq(OpCode.Invoke(res, fun,params,srcId))
          }
          override val src: SourceId = srcId
        }))
      case _ =>
        //todo: can this happen or is this an unexpected
        //  Try to provoke
        feedback(LocatedMessage("Can not resolve implement target",srcId,Error))
        None
    }
  }
}
