package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.types.{AdtType, AttrId, FetchMode, Func, Id, OpCode, Ref, SigType, SourceId, Type, TypedId}
import samaya.toolbox.track.TypeTracker

//todo: split into Untyped & Typed Arity Checker
trait ArityChecker extends TypeTracker{

  override def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
    if(code.filter(!_.isVirtual).last.rets.size != result.size) {
      feedback(LocatedMessage(s"Number of returned values from block body does not match number of expected values", origin, Error))
    }
    super.traverseBlockStart(input, result, code, origin, stack)
  }

  override def finalState(stack: Stack): Unit = {
    if(stack.stackSize() != results.size) {
      feedback(LocatedMessage("The function body returns a different number of arguments than expected according to the signature", component.fold(_.src,_.src), Error))
    }

    if(stack.frameSize != results.size) {
      unexpected("Only one frame was expected")
    }
    super.finalState(stack)
  }


  override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt:AdtType =>
        val ctrs = adt.ctrs(context)
        ctrs.get(ctr.name) match {
          //check that whe have the right amount of fields
          case Some(args) => if(args.size != fields.size) {
            feedback(LocatedMessage(s"Number of case arguments do not match number of constructor arguments", origin, Error))
          }
          case None => feedback(LocatedMessage(s"Case can not be matched to a constructor", origin, Error))
        }
      case _ =>
    }
    super.caseStart(fields, src, ctr, mode, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt:AdtType =>
        val ctrs = adt.ctrs(context)
        if(ctrs.size != 1) feedback(LocatedMessage(s"Can not unpacks values of types with multiple constructors", origin, Error))
        ctrs.headOption match {
          //check that whe have the right amount of fields
          case Some((_,fields)) => if(fields.size != res.size) {
            feedback(LocatedMessage(s"Can not unpack types with miss matching number of constructor arguments", origin, Error))
          }
          case None => //feedback already given
        }
      case _ =>
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt:AdtType =>
        val ctrs = adt.ctrs(context)
        if(ctrs.size != 1) feedback(LocatedMessage(s"Can not access fields of values of types with multiple constructors", origin, Error))
        ctrs.headOption match {
          //check that whe have the right amount of fields
          case Some((_,fields)) => if(!fields.contains(fieldName.name)) {
            feedback(LocatedMessage(s"Can not access missing constructor field", origin, Error))
          }
          case None => //feedback already given
        }
      case _ =>
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin:SourceId, stack: Stack):Stack = {
    res.typ match {
      case adt: AdtType => if(!adt.ctrs(context).contains(ctr.name)) {
        feedback(LocatedMessage(s"Specified constructor is missing", origin, Error))
      }
      case _ =>
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  //Todo: the other invokes

  def checkInvoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin:SourceId): Unit = {
    val paramInfo = func.paramInfo(context)
    if(paramInfo.size != params.size) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Wrong number of params supplied for function call", origin, Error))
      }
    }

    val retInfo = func.returnInfo(context)
    if(retInfo.size != res.size) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Function call returns wrong number of arguments", origin, Error))
      }
    }
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkInvoke(res,func, params, origin)
    super.invoke(res, func, params, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkInvoke(res,sig, params, origin)
      case _ =>
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  def checkTryInvoke(func: Func,  params:Seq[Boolean], succParams:Int, failParams:Int, origin:SourceId): Unit = {
    val paramInfo = func.paramInfo(context)
    if(paramInfo.size != params.size) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Not enough parameters supplied for function call", origin, Error))
      }
    }

    if(params.count(b => b) != failParams) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Function call has wrong number of essential parameters", origin, Error))
      }
    }

    val retInfo = func.returnInfo(context)
    if(retInfo.size != succParams) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Function call returns wrong number of arguments", origin, Error))
      }
    }
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    checkTryInvoke(func,  params.map(_._1), succ._1.size, fail._1.size, origin)
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkTryInvoke(sig,  params.map(_._1), succ._1.size, fail._1.size, origin)
      case _ =>
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    if(res.size != resTypes.size) {
      feedback(LocatedMessage(s"Number of rollback type arguments do not match number of returned values", origin, Error))
    }
    super.rollback(res, resTypes, params, origin, stack)
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    if(res.size != src.size) {
      feedback(LocatedMessage(s"Return opcode requires same amount of arguments and returns", origin, Error))
    }
    super._return(res, src, origin, stack)
  }
}
