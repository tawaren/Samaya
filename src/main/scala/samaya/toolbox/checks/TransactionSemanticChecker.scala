package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.types._
import samaya.toolbox.track.TypeTracker

trait TransactionSemanticChecker extends TypeTracker{

  private lazy val isTransactional = component match {
    case Left(value) => value.transactional
    case Right(value) => value.transactional
  }
  
  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    if(func.transactional(context) && !isTransactional) {
      feedback(LocatedMessage("A call to a transactional function is only allowed in a transactional function or implementation of a transactional signature", origin, Error))
    }
    super.invoke(res, func, params, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    stack.getType(src) match {
      case sig:SigType => if(sig.transactional(context) && !isTransactional) {
        feedback(LocatedMessage("A call to a transactional function is only allowed in a transactional function or implementation of a transactional signature", origin, Error))
      }
      case _ =>
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): State = {
    if(!func.transactional(context)) {
      feedback(LocatedMessage("A try call must target a transactional function", origin, Error))
    }
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): State = {
    stack.getType(src) match {
      case sig:SigType => if(!sig.transactional(context)) {
        feedback(LocatedMessage("A try call must target a transactional function", origin, Error))
      }
      case _ =>
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    if(!isTransactional) {
      feedback(LocatedMessage("Rollback is only allowed in a transactional function or implementation of a transactional signature", origin, Error))
    }
    super.rollback(res, resTypes, params, origin, stack)
  }
}
