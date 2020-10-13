package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.simple.MandalaParser
import samaya.structure.types.Permission.{Call, Define}
import samaya.structure.types._
import samaya.structure._
import samaya.toolbox.process.TypeInference

import scala.collection.JavaConverters._

trait SigCompiler extends CompilerToolbox {
  self: CapabilityCompiler with PermissionCompiler with ComponentBuilder with ComponentResolver with ExpressionBuilder =>

  private var classGenerics:Seq[Generic] = Seq.empty
  def withClassGenerics[T](generics:Seq[Generic])(body: => T):T = {
    val oldClassGenerics = classGenerics
    classGenerics = generics
    val res = body
    classGenerics = oldClassGenerics
    res
  }

  /*
  override def visitSig(ctx: MandalaParser.SigContext): SignatureDef = {
    val localGenerics = withDefaultCaps(dataCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }
    withGenerics(localGenerics) {
      val loc = sourceIdFromContext(ctx)
      val access = withSupportedPerms(Set(Call, Define)){
        visitAccessibilities(ctx.accessibilities())
      }
      registerSignatureDef(new SignatureDef {
        override val position: Int = nextPosition()
        override val src: SourceId = loc
        override val transactional: Boolean = ctx.TRANSACTIONAL() != null
        override val index: Int = nextSigIndex()
        override val name: String = visitName(ctx.name)
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission,Accessibility] = access
        override val capabilities: Set[Capability] = withDefaultCaps(sigCapsDefault){
          visitCapabilities(ctx.capabilities())
        }
        override val generics: Seq[Generic] = localGenerics

        override val params: Seq[Param] = withFreshIndex{
          ctx.params().p.asScala.map(visitParam)
        }
        override val results: Seq[Result] = withFreshIndex{
          ctx.rets().r.asScala.map(visitRet)
        }
      })
    }
  }

  override def visitImplement(ctx: MandalaParser.ImplementContext): ImplementDef = {
    val localGenerics = classGenerics ++ withDefaultCaps(dataCapsDefault) {
      visitGenericArgs(ctx.genericArgs())
    }

    withGenerics(localGenerics) {
      val loc = sourceIdFromContext(ctx)
      val access = withSupportedPerms(Set(Call)){
        visitAccessibilities(ctx.accessibilities())
      }
      val bodyBindings = ctx.captures.p.asScala.map(n => visitName(n.name)) ++ ctx.paramBindings.i.asScala.map(b => visitName(b.name()))
      val ext = ctx.EXTERNAL() != null
      //todo: make retBindings optional similar to rets on function

      val (retBindings, body) = if(ctx.retBindings != null) {
        val res = visitBindings(ctx.retBindings)
        val defaultIds = res.map(b => Id(b.name))
        val code = withDefaultReturns(defaultIds) {processBody(ctx.funBody, bodyBindings.toSet)}
        if(code.isDefined) {
          if(classGenerics.nonEmpty || ext){
            feedback(LocatedMessage("External and Class Implements can not have a body", sourceIdFromContext(ctx.funBody), Error))
          }
        }
        (res,code)
      } else {
        val code = processBody(ctx.funBody, bodyBindings.toSet)
        val last = code.get.filter(!_.isVirtual).last.rets

        val res = withFreshIndex{
          last.map(aid => new Binding {
            override val name: String = aid.id.name
            override val index: Int = nextIndex()
            override val attributes: Seq[Attribute] = Seq.empty
            override val src: SourceId = sourceIdFromContext(ctx)
          })
        }
        (res,code)
      }

      if(body.isEmpty && !ext && classGenerics.isEmpty) {
        feedback(LocatedMessage("Non External Implements need a body",loc,Error))
      }

      val implType = visitBaseRef(ctx.baseRef())
      registerImplementDef(new ImplementDef {
        override val position: Int = nextPosition()
        override val src: SourceId = loc
        override val code: Seq[OpCode] = body.getOrElse(Seq.empty)
        override val index: Int = nextImplIndex()
        override val name: String = visitName(ctx.name)
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission,Accessibility] = access
        override val generics: Seq[Generic] = localGenerics
        override val sigParamBindings:Seq[Binding] = visitBindings(ctx.paramBindings)
        override val sigResultBindings:Seq[Binding] = retBindings
        override val params: Seq[Param] = withFreshIndex{
          ctx.captures.p.asScala.map(visitSimpleParam)
        }
        override val results: Seq[Result] = Seq(new Result {
          override val name: String = visitName(ctx.name)
          override val index: Int = 0
          override val typ: Type = implType
          override val attributes: Seq[Attribute] = Seq.empty
          override val src: SourceId = sourceIdFromContext(ctx)
        })
        override val external: Boolean = ext

        override val transactional: Boolean = implType match {
          case sigType: SigType => sigType.transactional(context)
          case _ => false
        }
      })
    }
  }*/

  override def visitFunction(ctx: MandalaParser.FunctionContext): FunctionDef = {
    val localGenerics = classGenerics ++ withDefaultCaps(genericFunCapsDefault){
      visitGenericArgs(ctx.genericArgs())
    }

    val permSet:Set[Permission] = if(classGenerics.isEmpty) {
      Set(Call)
    } else {
      //we will derive signatures from this and thus the Define is eligable
      Set(Call, Define)
    }

    withGenerics(localGenerics) {
      val loc = sourceIdFromContext(ctx)
      val access = withSupportedPerms(permSet){
        visitAccessibilities(ctx.accessibilities())
      }
      val ext = ctx.EXTERNAL() != null
      val bodyBindings = ctx.params().p.asScala.map(n => visitName(n.name))
      val (resResults, body) = if(ctx.rets() != null) {
        val res = withFreshIndex{
          ctx.rets().r.asScala.map(visitRet)
        }
        val defaultIds = res.map(b => Id(b.name))
        val code = withDefaultReturns(defaultIds){processBody(ctx.funBody, bodyBindings.toSet)}
        if(code.isDefined) {
          if(classGenerics.nonEmpty || ext){
            feedback(LocatedMessage("External and Class Implements can not have a body", sourceIdFromContext(ctx.funBody), Error))
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
            override val typ: Type = new TypeInference.TypeVar()
            override val attributes: Seq[Attribute] = Seq.empty
            override val src: SourceId = sourceIdFromContext(ctx)
          })
        }
        (res,code)
      }

      if(body.isEmpty && !ext && classGenerics.isEmpty) {
        feedback(LocatedMessage("Non External Functions need a body",loc,Error))
      }

      registerFunctionDef(new FunctionDef {
        override val position: Int = nextPosition()
        override val src: SourceId = loc
        override val code: Seq[OpCode] = body.getOrElse(Seq.empty)
        override val external:Boolean = ext
        override val transactional: Boolean = ctx.TRANSACTIONAL() != null
        override val index: Int = nextFunIndex()
        override val name: String = visitName(ctx.name)
        override val attributes: Seq[Attribute] = Seq.empty
        override val accessibility: Map[Permission,Accessibility] = access
        override val generics: Seq[Generic] = localGenerics
        override val params: Seq[Param] = withFreshIndex{
          ctx.params().p.asScala.map(visitParam)
        }
        override val results: Seq[Result] = resResults
      })
    }
  }


  override def visitParam(ctx: MandalaParser.ParamContext): Param = {
    var attr = Seq.empty[Attribute]
    if(ctx.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Map.empty)
    if(ctx.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Map.empty)
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
    if(ctx.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Map.empty)
    if(ctx.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Map.empty)
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
        if(t.IMPLICIT() != null) attr = attr :+ Attribute(MandalaCompiler.Implicit_Attribute_Name, Map.empty)
        if(t.CONTEXT() != null) attr = attr :+ Attribute(MandalaCompiler.Context_Attribute_Name, Map.empty)
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
