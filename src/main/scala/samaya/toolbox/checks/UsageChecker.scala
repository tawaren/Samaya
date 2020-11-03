package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.types._
import samaya.toolbox.stack.SlotFrameStack
import samaya.toolbox.track.OwnershipTracker
import samaya.toolbox.track.OwnershipTracker.{Borrowed, Consumed, Locked, Owned}

import scala.collection.immutable.ListMap

//Todo: Check no Unknown Ownership (only after join enough?)
//Todo: If we want to make error msgs here intuitive we need to track value history and show where the others are
//      Alla: A Consume remembers where it was consumed
//            A Lock where it was locked etc...
trait UsageChecker extends OwnershipTracker{

  private def useInfo(src:Set[SourceId]):String = {
      src.map(_.origin.start.localRefString).mkString(",")
  }

  override def traverseBlockEnd(assigns: Seq[Id], origin: SourceId, stack: Stack): Stack = {
    val frame = stack.frameValues
    frame.take(assigns.size).map(stack.getStatus).foreach {
      case Borrowed => feedback(LocatedMessage(s"Can not return borrowed value from block", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not return locked value from block (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not return consumed value from block (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.traverseBlockEnd(assigns, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val paramVals = stack.resolveAll(params)
    if(paramVals.distinct.size != paramVals.size) {
      paramVals.groupBy(v => v).filter(_._2.size >= 2).map(_._2.map(_.src).toSet).foreach{ vs =>
        feedback(LocatedMessage(s"Value is used twice (${useInfo(vs)}) as a rollback parameter", origin, Error))
      }
    }

    params.map(stack.getStatus(_)).foreach {
      case Borrowed => feedback(LocatedMessage(s"Can not rollback borrowed value", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not rollback locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not rollback consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.rollback(res, resTypes, params, origin, stack)
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val paramVals = stack.resolveAll(src)
    if(paramVals.distinct.size != paramVals.size) {
      paramVals.groupBy(v => v).filter(_._2.size >= 2).map(_._2.map(_.src).toSet).foreach{ vs =>
        feedback(LocatedMessage(s"Value is used twice (${useInfo(vs)}) as a return parameter", origin, Error))
      }
    }

    src.map(r => (r,stack.getStatus(r))).foreach {
      case (r, Borrowed) => feedback(LocatedMessage(s"Can not use borrowed value as return argument", r.src, Error))
      case (r, Locked(_, lockPos)) => feedback(LocatedMessage(s"Can not use locked value as return argument (value was locked at: ${useInfo(lockPos)})", r.src, Error))
      case (r, Consumed(consumePos)) => feedback(LocatedMessage(s"Can not use consumed value as return argument (value was consumed at: ${useInfo(consumePos)})", r.src, Error))
      case _ =>
    }

    super._return(res, src, origin, stack)
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): SlotFrameStack = {
    stack.getStatus(src) match {
      case Borrowed if mode == FetchMode.Move => feedback(LocatedMessage(s"Can not move a borrowed value", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not move locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not move consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.fetch(res, src, mode, origin, stack)
  }

  override def discard(trg:Ref, origin:SourceId, stack: Stack):Stack = {
    stack.getStatus(trg) match {
      case Borrowed => feedback(LocatedMessage(s"Can not discard a borrowed value", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not discard locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not discard consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.discard(trg,origin,stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getStatus(src) match {
      case Borrowed if mode == FetchMode.Move => feedback(LocatedMessage(s"Can not move unpack a borrowed value", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not unpack locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not unpack consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getStatus(src) match {
      case Borrowed if mode == FetchMode.Move => feedback(LocatedMessage(s"Can not move field ${fieldName.name} of a borrowed value", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not access field ${fieldName.name} of a locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not access field ${fieldName.name} of a consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }


  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getStatus(src) match {
      case Borrowed if mode == FetchMode.Move => feedback(LocatedMessage(s"Can not move switch on a borrowed value", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not switch on a locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not switch on a consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    stack.getStatus(src) match {
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not inspect a locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not inspect a consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.inspectBefore(res, src, branches, origin, stack)
  }

  override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: Stack): Stack = {
    val paramVals = stack.resolveAll(srcs)
    if(mode == FetchMode.Move && paramVals.distinct.size != paramVals.size) {
      paramVals.groupBy(v => v).filter(_._2.size >= 2).map(_._2.map(_.src).toSet).foreach{ vs =>
        feedback(LocatedMessage(s"Value is used twice (${useInfo(vs)}) as a move pack parameter", origin, Error))
      }
    }
    srcs.map(stack.getStatus(_)).foreach {
      case Borrowed if mode == FetchMode.Move => feedback(LocatedMessage(s"Can not use a borrowed value as move pack param", origin, Error))
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not use a locked value as pack param (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not use a consumed value as pack param (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.pack(res,srcs,ctr,mode,origin,stack)
  }

  def checkInvoke(func: Func, params: Seq[Ref], origin: SourceId, stack: Stack):Unit = {
    val paramConsumeInfo = func.paramInfo(context).map(_._2)
    val paramVals = stack.resolveAll(params)
    if(paramVals.distinct.size != paramVals.size) {
      paramVals.groupBy(v => v).filter(_._2.size >= 2).map(_._2.map(_.src).toSet).foreach{ vs =>
        feedback(LocatedMessage(s"Value is used twice (${useInfo(vs)}) as a function call parameter", origin, Error))
      }
    }
    paramConsumeInfo.zip(params).foreach { case (consume, ref) =>
      stack.getStatus(stack.resolve(ref)) match {
        case Borrowed if consume => feedback(LocatedMessage(s"Can not use a borrowed value for a consumed argument in a function call", origin, Error))
        case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not use a locked value as an argument in a function call (value was locked at: ${useInfo(lockPos)})", origin, Error))
        case Consumed(consumePos) => feedback(LocatedMessage(s"Can not use a consumed value as an argument in a function call (value was consumed at: ${useInfo(consumePos)})", origin, Error))
        case _ =>
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
        stack.getStatus(src) match {
          case Borrowed => feedback(LocatedMessage(s"Can not use a borrowed value as target of a function call", origin, Error))
          case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not use a locked value as target of a function call (value was locked at: ${useInfo(lockPos)})", origin, Error))
          case Consumed(consumePos) => feedback(LocatedMessage(s"Can not use a consumed value as target of a function call (value was consumed at: ${useInfo(consumePos)})", origin, Error))
          case _ =>
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
    stack.getStatus(src) match {
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not project a locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not project a consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.project(res, src, origin, stack)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    stack.getStatus(src) match {
      case Locked(_, lockPos) => feedback(LocatedMessage(s"Can not unproject a locked value (value was locked at: ${useInfo(lockPos)})", origin, Error))
      case Consumed(consumePos) => feedback(LocatedMessage(s"Can not unproject a consumed value (value was consumed at: ${useInfo(consumePos)})", origin, Error))
      case _ =>
    }
    super.unproject(res, src, origin, stack)
  }

}
