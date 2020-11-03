package samaya.toolbox.checks

import samaya.structure.types.{AttrId, Const, DefinedFunc, FetchMode, Func, Id, OpCode, Permission, Ref, SourceId, Type, TypedId}
import samaya.compilation.ErrorManager._
import samaya.structure.FunctionSig
import samaya.toolbox.track.TypeTracker

import scala.collection.immutable.ListMap

trait AccessibilityChecker extends TypeTracker{
  def isSystem:Boolean

  private val gens = entry match {
    case Left(value) => value.generics.map(_.name)
    case Right(value) => value.generics.map(_.name)
  }

  override def initialState(): Stack = {
    entry match {
      case Left(_) =>
      case Right(impl) => if(!isSystem && !impl.implements.hasPermission(context, Permission.Define)){
        val modName = context.module.map(_.name+".").getOrElse("")
        val funName = context.pkg.name+"."+modName+name
        feedback(LocatedMessage(s"Implement $funName lacks define permission for signature ${impl.implements.prettyString(context,gens)}", impl.src, Error))
      }
    }
    super.initialState()
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: State): State = {
    if(!isSystem && !res.typ.hasPermission(context, Permission.Create)){
      if(!res.typ.isUnknown) {
        feedback(LocatedMessage(s"Create permission for type ${res.typ.prettyString(context,gens)} is missing (required for pack opcode)", origin, Error))
      }
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  override def lit(res: TypedId, value: Const, origin: SourceId, stack: State): State = {
    if(!isSystem && !res.typ.hasPermission(context, Permission.Create)){
      if(!res.typ.isUnknown) {
        feedback(LocatedMessage(s"Create permission for type ${res.typ.prettyString(context,gens)} is missing (required for lit opcode)", origin, Error))
      }
    }
    super.lit(res, value, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    mode match {
      case FetchMode.Move => if(!isSystem && !typ.hasPermission(context, Permission.Consume)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage(s"Consume permission for type ${typ.prettyString(context,gens)} is missing (required for move unpack opcode)", origin, Error))
        }
      }
      case FetchMode.Copy => if(!isSystem && !typ.hasPermission(context, Permission.Inspect)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage(s"Inspect permission for type ${typ.prettyString(context,gens)} is missing (required for copy unpack opcode)", origin, Error))
        }
      }
      case FetchMode.Infer => if(!isSystem && !typ.hasPermission(context, Permission.Inspect) && !typ.hasPermission(context, Permission.Consume)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage(s"Inspect or consume permission for type ${typ.prettyString(context,gens)} is missing (required for unpack opcode)", origin, Error))
        }
      }
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    if(!isSystem && !typ.hasPermission(context, Permission.Consume)){
      if(!typ.isUnknown) {
      }
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    if(!isSystem && !typ.hasPermission(context, Permission.Inspect)){
      if(!typ.isUnknown) {
        feedback(LocatedMessage(s"Inspect permission for type ${typ.prettyString(context,gens)} is missing (required for inspect opcode)", origin, Error))
      }
    }
    super.inspectBefore(res, src, branches, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    mode match {
      case FetchMode.Move => if(!isSystem && !typ.hasPermission(context, Permission.Consume)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage(s"Consume permission for type ${typ.prettyString(context,gens)} is missing (required for move field opcode)", origin, Error))
        }
      }
      case FetchMode.Copy => if(!isSystem && !typ.hasPermission(context, Permission.Inspect)){
        if(!typ.isUnknown) {
          feedback(LocatedMessage(s"Inspect permission for type ${typ.prettyString(context,gens)} is missing (required for copy field opcode)", origin, Error))
        }
      }
      case FetchMode.Infer => if(!isSystem && !typ.hasPermission(context, Permission.Inspect)) {
        if(!typ.isUnknown) {
          feedback(LocatedMessage(s"Inspect or consume permission for type ${typ.prettyString(context,gens)} is missing (required for field opcode)", origin, Error))
        }
      }
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    if(!isSystem && !func.hasPermission(context, Permission.Call)){
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Call permission for function ${func.prettyString(context,gens)} is missing (required for invoke opcode)", origin, Error))
      }
    }
    super.invoke(res, func, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): State = {
    if(!isSystem && !func.hasPermission(context, Permission.Call)){
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"Call permission for function ${func.prettyString(context,gens)} is missing (required for try invoke opcode)", origin, Error))
      }
    }
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    if(!isSystem && !typ.hasPermission(context, Permission.Call)){
      if(!typ.isUnknown) {
        feedback(LocatedMessage(s"Call permission for signature ${typ.prettyString(context,gens)} is missing (required for invoke sig opcode)", origin, Error))
      }
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): State = {
    val typ = stack.getType(src)
    if(!isSystem && !typ.hasPermission(context, Permission.Call)){
      if(!typ.isUnknown) {
        feedback(LocatedMessage(s"Call permission for signature ${typ.prettyString(context,gens)} is missing (required for try invoke sig opcode)", origin, Error))
      }
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }
}
