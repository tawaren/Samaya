package samaya.toolbox.process

import samaya.compilation.ErrorManager.{Error, LocatedMessage, PlainMessage, feedback, producesErrorValue}
import samaya.structure.types.OpCode.{VirtualOpcode, ZeroSrcOpcodes}
import samaya.structure.types.{SourceId, _}
import samaya.structure.{Param, _}
import samaya.toolbox.track.{JoinType, TypeTracker}
import samaya.toolbox.transform.{ComponentTransformer, TransformTraverser}
import samaya.toolbox.traverse.ViewTraverser
import samaya.types.Context

object TypeInference extends ComponentTransformer {

  case class TypeHint(src: Ref, typ: Type, id: SourceId) extends VirtualOpcode with ZeroSrcOpcodes

  class TypeVar extends Type.Unknown(Set.empty) {
    override def equals(obj: Any): Boolean = {
      if (!obj.isInstanceOf[TypeVar]) return false
      this.eq(obj.asInstanceOf[AnyRef])
    }
  }

  override def transformFunction(in: FunctionDef, context: Context): FunctionDef = {
    val analyzer = new TypeInference(Left(in), context)
    val result = analyzer.extract()
    val transformer = new TypeReplacing(result, Left(in), context)
    val adapted = transformer.component.left.get
    new FunctionDef {
      override val src: SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = adapted.generics
      override val params: Seq[Param] = adapted.params
      override val results: Seq[Result] = adapted.results
      override val transactional: Boolean = in.transactional
      override val position: Int = in.position
    }
  }


  override def transformImplement(in: ImplementDef, context: Context): ImplementDef = {
    val analyzer = new TypeInference(Right(in), context)
    val result = analyzer.extract()
    val transformer = new TypeReplacing(result, Right(in), context)
    val adapted = transformer.component.right.get
    new ImplementDef {
      override val src: SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = adapted.generics
      override val params: Seq[Param] = adapted.params
      override val results: Seq[Result] = adapted.results
      override val position: Int = in.position
      override val sigParamBindings: Seq[Binding] = adapted.sigParamBindings
      override val sigResultBindings: Seq[Binding] = adapted.sigResultBindings
      override val transactional: Boolean = in.transactional
    }
  }

  //Makes it extendable
  trait InferenceData {
    def substitutions: Map[TypeVar, Type]
    def joins: Map[JoinType, Type]
  }

  case class AnalysisResults(
    override val substitutions: Map[TypeVar, Type],
    override val joins: Map[JoinType, Type]
  ) extends InferenceData

  class TypeInference(override val component: Either[FunctionDef, ImplementDef], override val context: Context) extends ViewTraverser with TypeTracker {
    protected var substitutions: Map[TypeVar, Type] = Map.empty
    protected var joins: Map[JoinType, TypeVar] = Map.empty

    def resolveJoin(jt:JoinType):Type = {
      joins.get(jt) match {
        case Some(value) => value
        case None =>
          //we assosiate a typevar with each possible Join
          val res = new TypeVar()
          joins = joins.updated(jt,res)
          //we unify all instances with it
          jt.joined.foreach(unify(_,res))
          res
      }
    }

    def resolve(v: Type): Type = v match {
      case jt: JoinType => resolve(resolveJoin(jt))
      case tv: TypeVar if substitutions.contains(tv) => resolve(substitutions(tv))
      case t => t
    }

    def unify(src: Type, trg: Type): Unit = {
      (resolve(src), resolve(trg)) match {
        case (s, t) if s == t =>
        case (s: TypeVar, t) => substitutions = substitutions.updated(s, t)
        case (s, t: TypeVar) => substitutions = substitutions.updated(t, s)
        case (s: Type.Projected, t: Type.Projected) => unify(s.inner, t.inner)
        case (s, t) => s.applies.zip(t.applies).foreach(tt => unify(tt._1, tt._2))
      }
    }

    def finalizeType(t:Type):Type = resolve(t).replaceContainedTypes(finalizeType)

    def extract(): InferenceData = {
      super.traverse()
      //todo: folding with reuse would be more efficient
      val resolvedSubs = substitutions.map(kv => (kv._1, finalizeType(kv._1)))
      val resolvedJoins = joins.map(kv => (kv._1, finalizeType(kv._1)))
      AnalysisResults(resolvedSubs, resolvedJoins)
    }

    override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
      res.typ match {
        case adtType: AdtType =>
          val ctrs = adtType.ctrs(context)
          ctrs.get(ctr.name) match {
            case Some(value) =>
              val fields = value.values
              srcs.map(stack.getType).zip(fields).foreach {
                case (srcT, targT) => unify(srcT, targT)
                case _ =>
              }
            case None =>
          }
        case _ =>
      }
      super.pack(res, srcs, ctr, mode, origin, stack)
    }

    private def inferFunctionCall(func: Func, params: Seq[Ref], stack: Stack): Unit = {
      val paramInfo = func.paramInfo(context)
      paramInfo.map(_._1).zip(params).foreach {
        case (t, src) => unify(t, stack.getType(src))
      }
    }

    override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      inferFunctionCall(func, params, stack)
      super.invoke(res, func, params, origin, stack)
    }

    override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      stack.getType(src) match {
        case sig: SigType => inferFunctionCall(sig, params, stack)
        case _ =>
      }
      super.invokeSig(res, src, params, origin, stack)
    }

    override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      inferFunctionCall(func, params.map(_._2), stack)
      super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
    }

    override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      stack.getType(src) match {
        case sig: SigType => inferFunctionCall(sig, params.map(_._2), stack)
        case _ =>
      }
      super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
    }


    override def virtual(code: VirtualOpcode, stack: Stack): Stack = {
      code match {
        case TypeHint(src, typ, id) =>
          unify(stack.getType(src), typ)
          super.virtual(code, stack)
        case _ => super.virtual(code, stack)
      }
    }

    override def finalState(stack: Stack): Unit = {
      for ((v, r) <- stack.frameValues.zip(results.reverse)) {
        unify(stack.getType(v), r.typ)
      }
      super.finalState(stack)
    }

  }

  class TypeReplacing(lookup: InferenceData, prevComp: Either[FunctionDef, ImplementDef], override val context: Context) extends TransformTraverser with TypeTracker {

    private def substituteType(t:Type):Type = t match {
      case jt:JoinType => lookup.joins.getOrElse(jt,{
          feedback(PlainMessage(s"A type could not be inferred", Error))
          Type.DefaultUnknown
      })
      case tv:TypeVar => lookup.substitutions.getOrElse(tv,{
        feedback(PlainMessage(s"A type could not be inferred", Error))
        Type.DefaultUnknown
      })
      case p:Type.Projected => substituteType(p.inner).projected()
      case g:Type.GenericType => g
      case typ => typ.replaceContainedTypes(substituteType)
    }

    protected def substituteFunc(f:Func):Func = f.replaceContainedTypes(substituteType)

    private def replaceParams(ps:Seq[Param]):Seq[Param] = ps.map(p => new Param {
      override def name: String = p.name
      override def index: Int = p.index
      override def typ: Type = substituteType(p.typ)
      override def consumes: Boolean = p.consumes
      override def attributes: Seq[Attribute] = p.attributes
      override def src: SourceId = p.src
    })

    private def replaceResult(rs:Seq[Result]):Seq[Result] = rs.map(r => new Result {
      override def name: String = r.name
      override def index: Int = r.index
      override def typ: Type = substituteType(r.typ)
      override def attributes: Seq[Attribute] = r.attributes
      override def src: SourceId = r.src
    })

    override lazy val component: Either[FunctionDef, ImplementDef] = prevComp match {
      case Left(in) => Left(new FunctionDef {
        override val src: SourceId = in.src
        override val code: Seq[OpCode] = in.code
        override val external: Boolean = in.external
        override val index: Int = in.index
        override val name: String = in.name
        override val attributes: Seq[Attribute] = in.attributes
        override val accessibility: Map[Permission, Accessibility] = in.accessibility
        override val generics: Seq[Generic] = in.generics
        override val params: Seq[Param] = replaceParams(in.params)
        override val results: Seq[Result] = replaceResult(in.results)
        override val transactional: Boolean = in.transactional
        override val position: Int = in.position
      })

      case Right(in) => Right(new ImplementDef {
        override val src: SourceId = in.src
        override val code: Seq[OpCode] = in.code
        override val external: Boolean = in.external
        override val index: Int = in.index
        override val name: String = in.name
        override val attributes: Seq[Attribute] = in.attributes
        override val accessibility: Map[Permission, Accessibility] = in.accessibility
        override val generics: Seq[Generic] = in.generics
        override val params: Seq[Param] = replaceParams(in.params)
        override val results: Seq[Result] = replaceResult(in.results)
        override val position: Int = in.position
        override val sigParamBindings: Seq[Binding] = in.sigParamBindings
        override val sigResultBindings: Seq[Binding] = in.sigResultBindings
        override val transactional: Boolean = in.transactional
      })
    }

    override def transformLit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val typ = substituteType(res.typ)
      val lit = typ.asLitType match {
        case Some(litT) =>
          value.enforceSize(litT.size(context)) match {
            case Some(sizedValue) => sizedValue
            case None =>
              feedback(LocatedMessage(s"Provided literal $value can not be converted to a value of type $typ", origin, Error))
              value
          }
        case None =>
          if(!typ.isUnknown) {
            feedback(LocatedMessage(s"Can only generate literals for external types", origin, Error))
          }
          value
      }
      Some(Seq(OpCode.Lit(TypedId(res.id, res.attributes, typ), lit, origin)))
    }

    override def transformPack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val newRes = TypedId(res.id, res.attributes, substituteType(res.typ))
      Some(Seq(OpCode.Pack(newRes, srcs, ctr, mode, origin)))
    }

    override def transformInvoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val newFunc = substituteFunc(func)
      Some(Seq(OpCode.Invoke(res, newFunc, params, origin)))
    }

    override def transformTryInvoke(res: Seq[AttrId], func: Func, param: Seq[(Boolean, Ref)], success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val newFunc = substituteFunc(func)
      Some(Seq(OpCode.TryInvoke(res, newFunc, param, success, failure, origin)))
    }

    override def transformRollback(rets: Seq[AttrId], params: Seq[Ref], types: Seq[Type], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val newTypes = types.map(substituteType)
      Some(Seq(OpCode.RollBack(rets, params, newTypes, origin)))
    }

    override def transformVirtual(code:VirtualOpcode, stack: Stack): Option[Seq[OpCode]] = {
      code match {
        case TypeHint(trg: Ref, typ: Type, origin: SourceId) =>
          substituteType(typ) match {
            case _:Type.Unknown => feedback(LocatedMessage("Explicit type check failed",origin, Error))
            case _ =>
          }
          //Eliminate Type Check
          Some(Seq())
        case _ => None
      }
    }
  }
}