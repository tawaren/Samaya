package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.types._
import samaya.toolbox.stack.SlotFrameStack
import samaya.toolbox.track.OwnershipTracker

import scala.collection.immutable.ListMap

//Todo: Check no Unknown Ownership (only after join enough?)
trait UsageChecker extends OwnershipTracker{

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val frame = stack.frameValues
    frame.take(assigns.size).map(stack.getStatus).foreach { value =>
      if(!value.isActive) {
        if(!value.isUnknown) {
          feedback(LocatedMessage("Can not return an inactive value from a block", origin, Error))
        }
      }
      if(value.isBorrowed) {
        if(!value.isUnknown) {
          feedback(LocatedMessage("Can not return a borrowed value from a block", origin, Error))
        }
      }
    }
    super.traverseBlockEnd(assigns, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val paramVals = stack.resolveAll(params)
    if(paramVals.distinct.size != paramVals.size) feedback(LocatedMessage("Can not use a value twice as parameter to a rollback", origin, Error))

    params.map(stack.getStatus(_)).foreach { status =>
      if(!status.isActive) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Rollback targets an inactive value", origin, Error))
        }
      }
      if(status.isBorrowed) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Rollback targets a borrowed value", origin, Error))
        }
      }
    }
    super.rollback(res, resTypes, params, origin, stack)
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val paramVals = stack.resolveAll(src)
    if(paramVals.distinct.size != paramVals.size) feedback(LocatedMessage("Can not use a value twice as parameter to a return", origin, Error))

    src.map(r => (r,stack.getStatus(r))).foreach {
      case (r, status) =>
        val loc = r.src.map(new InputSourceId(_)).getOrElse(origin)
        if(!status.isActive) {
          if(!status.isUnknown) {
            feedback(LocatedMessage("Return targets an inactive value", loc, Error))
          }
        }
        if(status.isBorrowed) {
          if(!status.isUnknown) {
            feedback(LocatedMessage("Return targets a borrowed value", loc, Error))
          }
        }
    }

    super._return(res, src, origin, stack)
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): SlotFrameStack = {
    val status = stack.getStatus(src)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Fetch targets an inactive value", origin, Error))
      }
    }
    mode match {
      case FetchMode.Move => if(status.isBorrowed) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Fetch can not move a borrowed value", origin, Error))
        }
      }
      case FetchMode.Copy | FetchMode.Infer =>
    }
    super.fetch(res, src, mode, origin, stack)
  }

  override def discard(trg:Ref, origin:SourceId, stack: Stack):Stack = {
    val status = stack.getStatus(trg)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Discard targets an inactive value", origin, Error))
      }
    }
    if(status.isBorrowed) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Discard targets a borrowed value", origin, Error))
      }
    }
    super.discard(trg,origin,stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val status = stack.getStatus(src)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Unpack targets an inactive value", origin, Error))
      }
    }
    mode match {
      case FetchMode.Move => if(status.isBorrowed) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Unpack can not move a borrowed value", origin, Error))
        }
      }
      case FetchMode.Copy | FetchMode.Infer =>
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val status = stack.getStatus(src)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Field targets an inactive value", origin, Error))
      }
    }
    mode match {
      case FetchMode.Move => if(status.isBorrowed) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Field can not move a borrowed value", origin, Error))
        }
      }
      case FetchMode.Copy | FetchMode.Infer =>
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }


  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val status = stack.getStatus(src)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Switch targets an inactive value", origin, Error))
      }
    }
    mode match {
      case FetchMode.Move => if(status.isBorrowed) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Switch can not move a borrowed value", origin, Error))
        }
      }
      case FetchMode.Copy | FetchMode.Infer =>
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    val status = stack.getStatus(src)
    if(!status.isActive){
      if(!status.isUnknown) {
        feedback(LocatedMessage("Inspect targets an inactive value", origin, Error))
      }
    }
    super.inspectBefore(res, src, branches, origin, stack)
  }

  override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: Stack): Stack = {
    val paramVals = stack.resolveAll(srcs)
    if(mode == FetchMode.Move && paramVals.distinct.size != paramVals.size) feedback(LocatedMessage("Can not use a value twice as parameter to a pack", origin, Error))
    srcs.map(stack.getStatus(_)).foreach { value =>
      if(!value.isActive) {
        if(!value.isUnknown) {
          feedback(LocatedMessage("Pack targets an inactive value", origin, Error))
        }
      }
      mode match {
        case FetchMode.Move => if(value.isBorrowed){
          if(!value.isUnknown) {
            feedback(LocatedMessage("Pack can not move a borrowed value", origin, Error))
          }
        }
        case FetchMode.Copy | FetchMode.Infer =>
      }
    }
    super.pack(res,srcs,ctr,mode,origin,stack)
  }

  def checkInvoke(func: Func, params: Seq[Ref], origin: SourceId, stack: Stack):Unit = {
    val paramConsumeInfo = func.paramInfo(context).map(_._2)
    val paramVals = stack.resolveAll(params)
    if(paramVals.distinct.size != paramVals.size) feedback(LocatedMessage("Can not use a value twice as parameter to a function", origin, Error))
    paramConsumeInfo.zip(params).foreach { case (consume, ref) =>
      val status = stack.getStatus(stack.resolve(ref))
      if(!status.isActive) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Invoke targets an inactive value", origin, Error))
        }
      }
      if(consume && status.isBorrowed) {
        if(!status.isUnknown) {
          feedback(LocatedMessage("Invoke can not consume borrowed value", origin, Error))
        }
      }
    }
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkInvoke(func,params,origin,stack)
    super.invoke(res, func, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    checkInvoke(func,params.map(_._2),origin,stack)
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }


  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig: SigType =>
        val status = stack.getStatus(src)
        if(!status.isActive) {
          if(!status.isUnknown) {
            feedback(LocatedMessage("Signature invocation targets an inactive value", origin, Error))
          }
        }
        if(status.isBorrowed){
          if(!status.isUnknown) {
            feedback(LocatedMessage("Signature invocation can not target a borrowed value", origin, Error))
          }
        }
        checkInvoke(sig,params,origin,stack)
      case _ =>
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig: SigType => checkInvoke(sig,params.map(_._2),origin,stack)
      case _ =>
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val status = stack.getStatus(src)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Project targets an inactive value", origin, Error))
      }
    }
    super.project(res, src, origin, stack)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val status = stack.getStatus(src)
    if(!status.isActive) {
      if(!status.isUnknown) {
        feedback(LocatedMessage("Unproject targets an inactive value", origin, Error))
      }
    }
    super.unproject(res, src, origin, stack)
  }

}
