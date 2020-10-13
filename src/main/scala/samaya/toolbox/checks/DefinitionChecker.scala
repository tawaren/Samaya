package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.TypeParameterized
import samaya.structure.types.Type.Projected
import samaya.structure.types._
import samaya.toolbox.track.{TypeTracker, ValueTracker}
import samaya.validation.SignatureValidator

import scala.collection.immutable.ListMap

//Todo: Check no Unknown Types (only after join enough?)
trait DefinitionChecker extends TypeTracker{
  override def finalState(stack: Stack): Unit = {
    //check results
    for((v,r) <- stack.frameValues.zip(results.reverse)) {
      //check type
      if (stack.getType(v).isUnknown){
        feedback(LocatedMessage(s"A value returned by the function has unknown type", component.fold(_.src,_.src), Error))
      }
    }
    super.finalState(stack)
  }



  override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
    //check type
    if (res.typ.isUnknown){
      feedback(LocatedMessage(s"The value returned by the lit opcode has unknown type", origin, Error))
    }
    super.lit(res, value, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    if (res.typ.isUnknown){
      feedback(LocatedMessage(s"The value returned by the pack opcode has unknown type", origin, Error))
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  private def checkFunctionCall(func: Func, origin:SourceId):Unit = {
    if (func.isUnknown){
      feedback(LocatedMessage(s"The called function is unknown", origin, Error))
    } else {
      for(ret <- func.returnInfo(context)){
        if(ret.isUnknown) {
          feedback(LocatedMessage(s"The value returned by a function call opcode has unknown type", origin, Error))
        }
      }
    }
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkFunctionCall(func, origin)
    super.invoke(res, func, params, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkFunctionCall(sig, origin)
      case _ =>
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    checkFunctionCall(func, origin)
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkFunctionCall(sig, origin)
      case _ =>
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt: AdtType => for(ctr <- adt.ctrs(context).values){
        ctr.get(fieldName.name) match {
          case Some(typ) => if(typ.isUnknown) {
              feedback(LocatedMessage(s"The fields returned by a field call opcode has unknown type", origin, Error))
          }
          case None =>
        }
      }
      case _ =>
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt: AdtType => for(ctr <- adt.ctrs(context).values; field <- ctr.values){
        if(field.isUnknown) {
          feedback(LocatedMessage(s"The fields returned by a unpack call opcode has unknown type", origin, Error))
        }
      }
      case _ =>
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def letAfter(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
    for(id <- res) {
      if(stack.getType(id).isUnknown){
        feedback(LocatedMessage(s"The value returned by a let opcode has unknown type", origin, Error))
      }
    }
    super.letAfter(res, block, origin, stack)
  }

  override def switchAfter(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    for(id <- res) {
      if(stack.getType(id).isUnknown){
        feedback(LocatedMessage(s"The value returned by a switch opcode has unknown type", origin, Error))
      }
    }
    super.switchAfter(res, src, branches, mode, origin, stack)
  }

  override def inspectAfter(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    for(id <- res) {
      if(stack.getType(id).isUnknown){
        feedback(LocatedMessage(s"The value returned by a inspect opcode has unknown type", origin, Error))
      }
    }
    super.inspectAfter(res, src, branches, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    for(ret <- resTypes){
      if(ret.isUnknown) {
        feedback(LocatedMessage(s"The value returned by a rollback opcode has unknown type", origin, Error))
      }
    }
    super.rollback(res, resTypes, params, origin, stack)
  }
}
