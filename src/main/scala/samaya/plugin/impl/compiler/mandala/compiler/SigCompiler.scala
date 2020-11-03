package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.mandala.{MandalaCompiler, MandalaParser}
import samaya.structure.types.Permission.{Call, Define}
import samaya.structure.types._
import samaya.structure._
import samaya.toolbox.process.TypeInference

import scala.collection.JavaConverters._

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
          feedback(LocatedMessage("Class Functions can not be overloaded", sourceId, Error))
        }
        if(localEntries.contains(rawName)) {
          feedback(LocatedMessage("Overloaded Functions can not share a name with non-overloaded function", sourceId, Error))
        }
      }
      val bodyBindings = ctx.params().p.asScala.map(n => visitName(n.name))
      val (resResults, body) = if(ctx.rets() != null) {
        val res = withFreshIndex{
          ctx.rets().r.asScala.map(visitRet)
        }
        val defaultIds = res.map(b => Id(b.name, b.src))
        val code = withDefaultReturns(defaultIds){processBody(ctx.funBody, bodyBindings.toSet)}
        if(code.isDefined) {
          if(componentGenerics.nonEmpty || ext){
            feedback(LocatedMessage("External and Class Functions can not have a body", sourceIdFromContext(ctx.funBody), Error))
          }

        }
        (res,code)
      } else {
        val code = processBody(ctx.funBody, bodyBindings.toSet)
        val last = code.get.filter(!_.isVirtual).last.rets
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

      if(body.isEmpty && !ext && componentGenerics.isEmpty) {
        feedback(LocatedMessage("Non External Functions need a body",sourceId,Error))
      }

      val funParams = withFreshIndex{
        ctx.params().p.asScala.map(visitParam)
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
        override val code: Seq[OpCode] = body.getOrElse(Seq.empty)
        override val external:Boolean = ext
        override val transactional: Boolean = ctx.TRANSACTIONAL() != null
        override val index: Int = nextFunIndex()
        override val name: String = funName
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission,Accessibility] = access
        override val generics: Seq[Generic] = localGenerics
        override val params: Seq[Param] = funParams
        override val results: Seq[Result] = resResults
      })
    }
  }


  override def visitParam(ctx: MandalaParser.ParamContext): Param = {
    var attr = Seq.empty[Attribute]
    if(ctx.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Attribute.Unit)
    if(ctx.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Attribute.Unit)
    new Param {
      override val name: String = visitName(ctx.name)
      override val index: Int = nextIndex()
      override val typ: Type = visitTypeRef(ctx.typeRef())
      override val consumes: Boolean = ctx.CONSUME() != null
      override val attributes: Seq[Attribute] = attr
      override val src: SourceId = sourceIdFromContext(ctx)
    }
  }

  override def visitSimpleParam(ctx: MandalaParser.SimpleParamContext): Param = {
    var attr = Seq.empty[Attribute]
    if(ctx.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Attribute.Unit)
    if(ctx.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Attribute.Unit)
    new Param {
      override val name: String = visitName(ctx.name)
      override val index: Int = nextIndex()
      override val typ: Type = visitTypeRef(ctx.typeRef())
      override val consumes: Boolean = true
      override val attributes: Seq[Attribute] = attr
      override val src: SourceId = sourceIdFromContext(ctx)
    }
  }


  override def visitRet(ctx: MandalaParser.RetContext): Result = new Result {
    override val name: String = visitName(ctx.name)
    override val index: Int = nextIndex()
    override val typ: Type = visitTypeRef(ctx.typeRef())
    override val attributes: Seq[Attribute] = Seq.empty
    override val src: SourceId = sourceIdFromContext(ctx)
  }

  private def visitBindings(ctx: MandalaParser.IdsContext): Seq[Binding] = {
    withFreshIndex{
      ctx.i.asScala.map(t => new Binding {
        override val name: String = visitName(t)
        override val index: Int = nextIndex()
        override val attributes: Seq[Attribute] = Seq.empty
        override val src: SourceId = sourceIdFromContext(ctx)
      })
    }
  }

  override def visitBindings(ctx: MandalaParser.BindingsContext): Seq[Binding] = {
    withFreshIndex{
      ctx.i.asScala.map(t => {
        var attr = Seq.empty[Attribute]
        if(t.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Attribute.Unit)
        if(t.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Attribute.Unit)
        new Binding {
          override val name: String = visitName(t.name())
          override val index: Int = nextIndex()
          override val attributes: Seq[Attribute] = attr
          override val src: SourceId = sourceIdFromContext(ctx)
        }
      })
    }
  }
}
