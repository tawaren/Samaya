package samaya.toolbox.checks

import samaya.structure.types.{AttrId, Const, DefinedFunc, FetchMode, Func, Id, OpCode, Permission, Ref, SourceId, Type, TypedId}
import samaya.compilation.ErrorManager._
import samaya.structure.FunctionSig
import samaya.toolbox.track.TypeTracker

import scala.collection.immutable.ListMap

trait AccessibilityChecker extends TypeTracker{
  def isSystem:Boolean

  override def initialState(): Stack = {
    component match {
      case Left(_) =>
      //todo: is this right here other better some where else
      case Right(impl) => if(!isSystem && !impl.implements.hasPermission(context, Permission.Define)){
        feedback(LocatedMessage("Can not implement signature as define permission for targeted type is missing", component.fold(_.src,_.src), Error))
      }
    }
    super.initialState()
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: State): State = {
    if(!isSystem && !res.typ.hasPermission(context, Permission.Create)){
      if(!res.typ.isUnknown) {
        feedback(LocatedMessage("Can not pack as create permission for targeted type is missing", origin, Error))
      }
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  override def lit(res: TypedId, value: Const, origin: SourceId, stack: State): State = {
    if(!isSystem && !res.typ.hasPermission(context, Permission.Create)){
      if(!res.typ.isUnknown) {
        feedback(LocatedMessage("Can not create lit as create permission for targeted type is missing", origin, Error))
      }
    }
    super.lit(res, value, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    mode match {
      case FetchMode.Move => if(!isSystem && !typ.hasPermission(context, Permission.Consume)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage("Can not move unpack value as consume permission for targeted type is missing", origin, Error))
        }
      }
      case FetchMode.Copy => if(!isSystem && !typ.hasPermission(context, Permission.Inspect)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage("Can not copy unpack value as inspect permission for targeted type is missing", origin, Error))
        }
      }
      case FetchMode.Infer => if(!isSystem && !typ.hasPermission(context, Permission.Inspect) && !typ.hasPermission(context, Permission.Consume)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage("Can not unpack value as inspect or consume permission for targeted type is missing", origin, Error))
        }
      }
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): State = {
    if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Consume)){
      if(!stack.getType(src).isUnknown) {
        feedback(LocatedMessage("Can not move switch value as consume permission for targeted type is missing", origin, Error))
      }
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): State = {
    if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Inspect)){
      if(!stack.getType(src).isUnknown) {
        feedback(LocatedMessage("Can not inspect value as inspect permission for targeted type is missing", origin, Error))
      }
    }
    super.inspectBefore(res, src, branches, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): State = {
    mode match {
      case FetchMode.Move => if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Consume)){
        if(!stack.getType(src).isUnknown) {
          feedback(LocatedMessage("Can not move field of value as consume permission for targeted type is missing", origin, Error))
        }
      }
      case FetchMode.Copy => if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Inspect)){
        if(!stack.getType(src).isUnknown) {
          feedback(LocatedMessage("Can not copy field of  value as inspect permission for targeted type is missing", origin, Error))
        }
      }
      case FetchMode.Infer => if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Inspect)) {
        if(!stack.getType(src).isUnknown) {
          feedback(LocatedMessage("Can not access field of value as inspect or consume permission for targeted type is missing", origin, Error))
        }
      }
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    if(!isSystem && !func.hasPermission(context, Permission.Call)){
      if(!func.isUnknown) {
        feedback(LocatedMessage("Can not call function as call permission for targeted type is missing", origin, Error))
      }
    }
    super.invoke(res, func, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): State = {
    if(!isSystem && !func.hasPermission(context, Permission.Call)){
      if(!func.isUnknown) {
        feedback(LocatedMessage("Can not call function as call permission for targeted type is missing", origin, Error))
      }
    }
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Call)){
      if(!stack.getType(src).isUnknown) {
        feedback(LocatedMessage("Can not apply value as call permission for targeted type is missing", origin, Error))
      }
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): State = {
    if(!isSystem && !stack.getType(src).hasPermission(context, Permission.Call)){
      if(!stack.getType(src).isUnknown) {
        feedback(LocatedMessage("Can not apply value as call permission for targeted type is missing", origin, Error))
      }
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }
}
