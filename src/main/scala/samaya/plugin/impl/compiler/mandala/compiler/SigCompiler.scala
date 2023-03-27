package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Compiler, Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.mandala.{MandalaCompiler, MandalaParser}
import samaya.structure.types.Permission.{Call, Define}
import samaya.structure.types._
import samaya.structure._
import samaya.toolbox.process.TypeInference

import scala.jdk.CollectionConverters._

trait SigCompiler extends CompilerToolbox {
  self: CapabilityCompiler with PermissionCompiler with ComponentBuilder with ComponentResolver with ExpressionBuilder =>

  override def visitFunction(ctx: MandalaParser.FunctionContext): FunctionDef = {
    val localGenerics = withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }

    val permSet:Set[Permission] = if(componentGenerics.isEmpty) {
      Set(Call)
    } else {
      //we will derive signatures from this and thus the Define is eligable
      Set(Call, Define)
    }

    withGenerics(localGenerics) {
      val sourceId = sourceIdFromContext(ctx)
      val access = withSupportedPerms(permSet){
        visitAccessibilities(ctx.accessibilities())
      }
      val rawName = visitName(ctx.name);
      val ext = ctx.EXTERNAL() != null
      val overloaded = ctx.OVERLOADED != null
      if(overloaded) {
        if(componentGenerics.nonEmpty){
          feedback(LocatedMessage("Class Functions can not be overloaded", sourceId, Error, Compiler()))
        }
        if(localEntries.contains(rawName)) {
          feedback(LocatedMessage("Overloaded Functions can not share a name with non-overloaded function", sourceId, Error, Compiler()))
        }
      }

      val (funParams, bodyBindings, processors) = visitParams(ctx.params())
      val (resResults, body) = if(ctx.rets() != null) {
        val res = withFreshIndex{
          ctx.rets().r.asScala.map(visitRet)
        }
        val defaultIds = res.map(b => Id(b.name, b.src)).toSeq
        val code = withDefaultReturns(defaultIds){
          processBody(ctx.funBody, bodyBindings)
        }
        if(code.isDefined) {
          if(componentGenerics.nonEmpty || ext){
            feedback(LocatedMessage("External and Class Functions can not have a body", sourceIdFromContext(ctx.funBody), Error, Compiler()))
          }

        }
        (res,code)
      } else {
        val code = processBody(ctx.funBody, bodyBindings)
        val nonVirtuals = code.get.filter(!_.isVirtual)
        if(nonVirtuals.isEmpty){
          feedback(LocatedMessage("Module Functions must have a body", sourceIdFromContext(ctx.funBody), Error, Compiler()))
          (Seq.empty,code)
        } else {
          val last = nonVirtuals.last.rets
          val res = withFreshIndex{
            last.map(aid => new Result {
              override val name: String = aid.id.name
              override val index: Int = nextIndex()
              override val typ: Type = TypeInference.TypeVar(sourceId)
              override val attributes: Seq[Attribute] = Seq.empty
              override val src: SourceId = sourceId
            })
          }
          (res,code)
        }
      }

      if(body.isEmpty && processors.nonEmpty) {
        feedback(LocatedMessage("External Functions can not have paramter extractors",sourceId,Error, Compiler()))
      }

      if(body.isEmpty && !ext && componentGenerics.isEmpty) {
        feedback(LocatedMessage("Non External Functions need a body",sourceId,Error, Compiler()))
      }

      val funName = if(overloaded){
        val nonImplicits = funParams.count(p => !p.attributes.exists(a => a.name.equals(MandalaCompiler.Implicit_Attribute_Name)))
        MandalaModule.deriveOverloadedName(rawName,nonImplicits);
      } else {
        rawName
      }

      registerFunctionDef(new FunctionDef {
        override val position: Int = nextPosition()
        override val src:SourceId = sourceId
        override val code: Seq[OpCode] = body.map(processors ++ _).getOrElse(Seq.empty)
        override val external:Boolean = ext
        override val transactional: Boolean = ctx.TRANSACTIONAL() != null
        override val index: Int = nextFunIndex()
        override val name: String = funName
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission,Accessibility] = access
        override val generics: Seq[Generic] = localGenerics
        override val params: Seq[Param] = funParams
        override val results: Seq[Result] = resResults.toSeq
      })
    }
  }

  override def visitParams(ctx: MandalaParser.ParamsContext): (Seq[Param], Set[String], Seq[OpCode]) = {
    withFreshIndex{
      ctx.p.asScala.map(visitParam).foldLeft(Seq.empty[Param],Set.empty[String], Seq.empty[OpCode]){
        case ((pAggr,bAggr,cAggr),(param,bindings,code)) => (pAggr :+ param, bAggr++bindings, cAggr++code)
      }
    }
  }

  override def visitParam(ctx: MandalaParser.ParamContext): (Param, Set[String], Seq[OpCode]) = {
    if(ctx.name() != null){
      var attr = Seq.empty[Attribute]
      if(ctx.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Attribute.Unit)
      if(ctx.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Attribute.Unit)
      val param:Param = new Param {
        override val name: String = visitName(ctx.name)
        override val index: Int =  nextIndex()
        override val typ: Type = visitTypeRef(ctx.typeRef())
        override val consumes: Boolean = ctx.CONSUME() != null
        override val attributes: Seq[Attribute] = attr
        override val src: SourceId = sourceIdFromContext(ctx)
      }
      (param, Set(param.name), Seq.empty)
    } else {
      withInspectMode(ctx.CONSUME() == null){
        val (id, bindings, code, paramType) = visitPatternBinding(ctx, ctx.typeRef(), ctx.patterns())
        val param:Param = new Param {
          override val name: String = id.name
          override val index: Int = nextIndex()
          override val typ: Type = paramType
          override val consumes: Boolean = ctx.CONSUME() != null
          override val attributes: Seq[Attribute] = id.attributes
          override val src: SourceId = sourceIdFromContext(ctx)
        }
        (param, bindings, code)
      }
    }

  }

  override def visitRet(ctx: MandalaParser.RetContext): Result = new Result {
    override val name: String = visitName(ctx.name)
    override val index: Int = nextIndex()
    override val typ: Type = visitTypeRef(ctx.typeRef())
    override val attributes: Seq[Attribute] = Seq.empty
    override val src: SourceId = sourceIdFromContext(ctx)
  }
}
