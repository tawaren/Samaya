package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Compiler, Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.plugin.impl.compiler.mandala.components.clazz.{Class, MandalaFunClassCompilerOutput, MandalaSigClassCompilerOutput}
import samaya.structure.{Attribute, DataDef, FunctionDef, Generic, Module, ModuleEntry, Param, Result, SignatureDef}
import samaya.structure.types.Accessibility.Global
import samaya.structure.types.{Accessibility, Capability, CompLink, OpCode, Permission, SourceId}

import scala.jdk.CollectionConverters._

trait ClassBuilder  extends CompilerToolbox{
  self: ComponentResolver with ComponentBuilder with CapabilityCompiler with PermissionCompiler with SigCompiler =>

  override def visitClass_(ctx: MandalaParser.Class_Context): Unit = {
    val name = visitName(ctx.name)
    val generics = withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }
    if(generics.isEmpty) {
      feedback(LocatedMessage("Classes must have at least one generic arguments (use a module otherwise)", sourceIdFromContext(ctx), Error, Compiler()))
    }
    withComponentBuilder(name) {
      withDefaultAccess(Global) {
        withComponentGenerics(generics){
          ctx.classEntry().asScala.map(visitClassEntry)
        }
      }
      val src = sourceIdFromContext(ctx)
      val rawFunComponents = currentComponent._functions
      val funComponent = funClass(name, generics, rawFunComponents, src)
      val mode = if(ctx.SYSTEM != null) Module.Elevated else Module.Normal
      build(funComponent, src) match {
        case Some(funClsInter) =>
          val SigComponents = withComponentBuilder(name) {
            if(funClsInter.link.isInstanceOf[CompLink.ByCode]){
              println(funClsInter.getClass)
              println("Ups")
            }
            sigClass(name, generics, rawFunComponents, funClsInter.link, mode, src)
          }
          build(SigComponents, src)
        case None => //Todo: Error???
      }
    }
  }

  override def visitClassEntry(ctx: MandalaParser.ClassEntryContext): Unit = {
    withFreshCounters { super.visitClassEntry(ctx) }
  }

  private def funClass(name:String, generics: Seq[Generic], components: Seq[FunctionDef], sourceId: SourceId): Class = {
    val funs = components.map(fd => new FunctionDef {
        override val position: Int = fd.position
        override val src: SourceId = fd.src
        override val transactional: Boolean = fd.transactional
        override val index: Int = fd.index
        override val name: String = fd.name
        override val attributes: Seq[Attribute] = fd.attributes
        override val accessibility: Map[Permission,Accessibility] = fd.accessibility - Permission.Define
        override val generics: Seq[Generic] = fd.generics
        override val params: Seq[Param] = fd.params
        override val results: Seq[Result] = fd.results
        override val code: Seq[OpCode] = fd.code
        override val external: Boolean = fd.external
    })

    new MandalaFunClassCompilerOutput(
      name,
      Module.Normal, //The Module Type will be set on the SigPart where it is relevant
      generics,
      funs,
      sourceId
    )
  }

  private def sigClass(name:String, generics: Seq[Generic], components: Seq[ModuleEntry], clazzLink:CompLink, mode:Module.Mode, sourceId: SourceId): Class = {
    val sigs = components.flatMap {
      case fd:FunctionDef => Some(registerSignatureDef(new SignatureDef {
        override val position: Int = nextPosition()
        override val src: SourceId = fd.src
        override val transactional: Boolean = fd.transactional
        override val index: Int = nextSigIndex()
        override val name: String = fd.name
        override val attributes: Seq[Attribute] = fd.attributes
        override val accessibility: Map[Permission,Accessibility] = fd.accessibility
        override val capabilities: Set[Capability] = Set(Capability.Drop)
        override val generics: Seq[Generic] = fd.generics
        override val params: Seq[Param] = fd.params
        override val results: Seq[Result] = fd.results
      }))
      case _ => None
    }
    new MandalaSigClassCompilerOutput(
      name,
      mode,
      clazzLink,
      generics,
      sigs,
      sourceId
    )
  }

}
