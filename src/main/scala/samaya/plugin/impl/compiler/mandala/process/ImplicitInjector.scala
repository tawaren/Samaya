package samaya.plugin.impl.compiler.mandala.process

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.clazz.SigClass
import samaya.structure.types._
import samaya.structure._
import samaya.toolbox.track.TypeTracker
import samaya.toolbox.transform.{EntryTransformer, TransformTraverser}
import samaya.types.Context

import scala.collection.immutable.ListMap

class ImplicitInjector(instancesFinder:InstanceFinder)  extends EntryTransformer{

  override def transformFunction(in: FunctionDef, context: Context): FunctionDef = {
    val transformer = new ImplicitInjector(Left(in), context)
    new FunctionDef {
      override val src:SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = in.generics
      override val params: Seq[Param] = in.params
      override val results: Seq[Result] = in.results
      override val transactional: Boolean = in.transactional
      override val position: Int = in.position
    }
  }

  override def transformImplement(in: ImplementDef, context: Context): ImplementDef = {
    val transformer = new ImplicitInjector(Right(in), context)
    new ImplementDef {
      override val src:SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = in.generics
      override val params: Seq[Param] = in.params
      override val results: Seq[Result] = in.results
      override val position: Int = in.position
      override val sigParamBindings: Seq[Binding] = in.sigParamBindings
      override val sigResultBindings: Seq[Binding] = in.sigResultBindings
      override val transactional: Boolean = in.transactional
    }
  }

  class ImplicitInjector(override val entry: Either[FunctionDef, ImplementDef], override val context: Context) extends TransformTraverser with TypeTracker {

    var providedStack:Seq[Map[Type,Val]] = Seq(Map.empty)
    private def providedMap():Map[Type,Val] = providedStack.head
    private def record(typ:Type, place:Val): Unit = {
      if(providedStack.head.contains(typ)) {
        feedback(LocatedMessage(s"The context value $place shadows previous context value ${providedStack.head(typ)} of the same type",place.src,Warning))
      }
      providedStack = providedStack.head.updated(typ, place) +: providedStack.tail
    }

    private def record(aid:AttrId, stack:State): Unit = {
      if(aid.attributes.exists(a => a.name == MandalaCompiler.Context_Attribute_Name)){
        val value = stack.resolve(aid)
        val typ = stack.getType(value)
        record(typ, value)
      }
    }

    private def record(tid:TypedId, stack:State): Unit = {
      if(tid.attributes.exists(a => a.name == MandalaCompiler.Context_Attribute_Name)){
        val value = stack.resolve(tid.id)
        val typ = stack.getType(value)
        record(typ, value)
      }
    }

    private def recordAll(ids:Seq[AttrId], stack:State): Unit = ids.foreach(record(_,stack))

    override def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, stack: State): State = {
      providedStack = providedStack.head +: providedStack
      super.traverseBlockStart(input, result, code, origin, stack)
    }

    override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: State): State = {
      providedStack = providedStack.tail
      super.traverseBlockEnd(assigns, origin, stack)
    }

    override def functionStart(params: Seq[Param], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.functionStart(params, origin, stack)
      for(p <- params) {
        if(p.attributes.exists(a => a.name == MandalaCompiler.Context_Attribute_Name)){
          val value = nStack.resolve(getParamVal(p))
          val typ = nStack.getType(value)
          record(typ, value)
        }
      }
      nStack
    }

    override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.caseStart(fields, src, ctr, mode, origin, stack)
      recordAll(fields, nStack)
      nStack
    }

    override def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.invokeSuccStart(fields, call, origin, stack)
      recordAll(fields, nStack)
      nStack
    }

    override def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, stack: Stack): Stack = {
      val nStack = super.invokeFailStart(fields, call, essential, origin, stack)
      recordAll(fields, nStack)
      nStack
    }

    override def lit(res:TypedId, value:Const, origin:SourceId, stack: State): State = {
      val nStack = super.lit(res,value,origin,stack)
      record(res, nStack)
      nStack
    }

    override def letAfter(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: State): State = {
      val nStack = super.letAfter(res,block,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: State): State = {
      val nStack = super.fetch(res,src,mode,origin,stack)
      record(res, nStack)
      nStack
    }

    override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: State): State = {
      val nStack = super._return(res,src,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: State): State = {
      val nStack = super.unpack(res,src,mode,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def switchAfter(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: State): State = {
      val nStack = super.switchAfter(res,src,branches,mode,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def inspectAfter(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: State): State = {
      val nStack = super.inspectAfter(res,src,branches,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: State): State = {
      val nStack = super.field(res,src,fieldName,mode,origin,stack)
      record(res, nStack)
      nStack
    }

    override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: State): State = {
      val nStack = super.pack(res,srcs,ctr,mode,origin,stack)
      record(res, nStack)
      nStack
    }

    private def resolveParams(params:Seq[Ref], func:Func):(Seq[OpCode], Seq[Ref]) = {
      val funcParams = func.paramInfo(context)
      val target = func match {
        case func: DefinedFunc[_] => func.getEntry(context)
        case sigType: SigType => sigType.getEntry(context)
        case _ => return (Seq.empty, params)
      }

      var producingOpcodes:Seq[OpCode] = Seq.empty;
      val newParams = target match {
        case Some(sig:FunctionSig) => params ++ sig.params.zip(funcParams).drop(params.size).map{
          case (p, (typ,_)) =>
            if(p.attributes.exists(a => a.name == MandalaCompiler.Implicit_Attribute_Name)) {
              val map = providedMap()
              if(map.contains(typ)) {
                map(typ)
              } else {
                typ match {
                  case sigType: SigType => (sigType.getComponent(context), sigType.getEntry(context)) match {
                    case (Some(cls:SigClass),Some(entry)) =>
                      val (clsApplies,funApplies) = typ.applies.splitAt(cls.generics.size)
                      instancesFinder.findAndApplyImplementFunction(entry.name, cls.clazzLink, clsApplies, funApplies, context, p.src) match {
                        case Some(implement) =>
                          val (parmCodes, params) = resolveParams(Seq.empty, implement)
                          producingOpcodes = producingOpcodes ++ parmCodes
                          val ret = Id(p.src)
                          val code = OpCode.Invoke(Seq(AttrId(ret, Seq.empty)),implement,params, p.src)
                          producingOpcodes = producingOpcodes :+ code
                          ret
                        case None =>
                          feedback(LocatedMessage(s"Can not find or generate implicit for ${typ.prettyString(context)}",p.src,Error))
                          Val.unknown(p.name, p.src)
                      }
                    case _ =>
                      feedback(LocatedMessage(s"Can not find or generate implicit for ${typ.prettyString(context)}",p.src,Error))
                      Val.unknown(p.name, p.src)
                  }
                  case _ =>
                    feedback(LocatedMessage(s"Can not find or generate implicit for ${typ.prettyString(context)}",p.src,Error))
                    Val.unknown(p.name, p.src)
                }
              }
            } else {
              feedback(LocatedMessage("Only implicit parameters are allowed to be missing",p.src,Error))
              Val.unknown(p.name, p.src)
            }
        }
        case _ => params
      }
      (producingOpcodes, newParams)
    }


    override def transformInvoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val (pCodes, nParams) = resolveParams(params, func)
      Some(pCodes :+ OpCode.Invoke(res,func,nParams,origin))
    }

    override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: State): State = {
      val nStack = super.invoke(res,func,params,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def transformInvokeSig(res: Seq[AttrId], func: Ref, param: Seq[Ref], origin: SourceId, stack: Stack): Option[Seq[OpCode]] =  {
      stack.getType(func) match {
        case sigFunc:SigType =>
          val (pCodes, nParams) = resolveParams(param, sigFunc)
          Some(pCodes :+ OpCode.InvokeSig(res,func,nParams,origin))
        case _ => None
      }
    }

    override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: State): State = {
      val nStack = super.invokeSig(res,src,params,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def transformTryInvoke(res: Seq[AttrId], func: Func, param: Seq[(Boolean, Ref)], success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val (pCodes, nParams) = resolveParams(param.map(_._2), func)
      val nFinParams = param.map(_._1).zipAll(nParams, false, Val.unknown("unknown",origin))
      Some(pCodes :+ OpCode.TryInvoke(res,func,nFinParams, success,failure,origin))
    }

    override def tryInvokeAfter(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: State): State = {
      val nStack = super.tryInvokeAfter(res,func,params,succ,fail,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def transformTryInvokeSig(res: Seq[AttrId], func: Ref, param: Seq[(Boolean, Ref)], origin: SourceId, stack: Stack, success: (Seq[AttrId], Seq[OpCode]), failure: (Seq[AttrId], Seq[OpCode])): Option[Seq[OpCode]] = {
      stack.getType(func) match {
        case sigFunc:SigType =>
          val (pCodes, nParams) = resolveParams(param.map(_._2), sigFunc)
          val nFinParams = param.map(_._1).zipAll(nParams, false, Val.unknown("unknown",origin))
          Some(pCodes :+ OpCode.TryInvokeSig(res, func, nFinParams, success, failure, origin))
        case _ => None
      }
    }

    override def tryInvokeSigAfter(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: State): State = {
      val nStack = super.tryInvokeSigAfter(res,src,params,succ,fail,origin,stack)
      recordAll(res, nStack)
      nStack
    }

    override def project(res: AttrId, src: Ref, origin: SourceId, stack: State): State = {
      val nStack = super.project(res,src,origin,stack)
      record(res, nStack)
      nStack
    }

    override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: State): State = {
      val nStack = super.unproject(res,src,origin,stack)
      record(res, nStack)
      nStack
    }

    override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: State): State = {
      val nStack = super.rollback(res,resTypes,params,origin,stack)
      recordAll(res, nStack)
      nStack
    }
  }

}
