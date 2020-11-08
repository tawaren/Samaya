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

  private val gens = entry match {
    case Left(value) => value.generics.map(_.name)
    case Right(value) => value.generics.map(_.name)
  }

  private def indexToString(num:Int):String ={
    num match {
      case 1 => "first"
      case 2 => "second"
      case 3 => "third"
      case _ => num+"th"
    }
  }

  override def finalState(stack: Stack): Unit = {
    //check results
    for((v, idx) <- stack.frameValues.take(results.size).zipWithIndex) {
      //check type
      if (stack.getType(v).isUnknown){
        val modName = context.module.map(_.name+".").getOrElse("")
        val funName = context.pkg.name+"."+modName+name
        feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the function $funName has unknown type", v.src, Error))
      }
    }
    super.finalState(stack)
  }



  override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
    //check type
    if (res.typ.isUnknown){
      feedback(LocatedMessage(s"The value returned by the lit opcode has unknown type", res.id.src, Error))
    }
    super.lit(res, value, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    if (res.typ.isUnknown){
      feedback(LocatedMessage(s"The value returned by the pack opcode has unknown type", res.id.src, Error))
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  private def checkFunctionCall(func: Func, origin:SourceId):Unit = {
    if (func.isUnknown){
      feedback(LocatedMessage(s"The called function is unknown", func.src, Error))
    } else {
      for((ret,idx) <- func.returnInfo(context).zipWithIndex){
        if(ret.isUnknown) {
          feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the function call has unknown type", ret.src, Error))
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
    stack.getType(src).projectionExtract {
      case adt: AdtType => for((ctr, idx) <- adt.ctrs(context).values.zipWithIndex){
        ctr.get(fieldName.name) match {
          case Some(typ) => if(typ.isUnknown) {
              feedback(LocatedMessage(s"The ${indexToString(idx+1)})($name) field of ${adt.prettyString(context,gens)} has unknown type", fieldName.src, Error))
          }
          case None =>
        }
      }
      case _ =>
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract {
      case adt: AdtType => for(ctr <- adt.ctrs(context).values; ((name,field), idx) <- ctr.zipWithIndex){
        if(field.isUnknown) {
          val src = res.lift(idx).map(_.id.src).getOrElse(origin)
          feedback(LocatedMessage(s"The ${indexToString(idx+1)})($name) field of ${adt.prettyString(context,gens)} unknown type", src, Error))
        }
      }
      case _ =>
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def letAfter(res: Seq[AttrId], block: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
    for((id,idx) <- res.zipWithIndex) {
      if(stack.getType(id).isUnknown){
        feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the let opcode has unknown type", id.src, Error))
      }
    }
    super.letAfter(res, block, origin, stack)
  }

  override def switchAfter(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    for((id,idx) <- res.zipWithIndex) {
      if(stack.getType(id).isUnknown){
        feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the switch opcode has unknown type", id.src, Error))
      }
    }
    super.switchAfter(res, src, branches, mode, origin, stack)
  }

  override def inspectAfter(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    for((id,idx) <- res.zipWithIndex) {
      if(stack.getType(id).isUnknown){
        feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the inspect opcode has unknown type", id.src, Error))
      }
    }
    super.inspectAfter(res, src, branches, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    for(((id,ret),idx) <- res.zip(resTypes).zipWithIndex) {
      if(ret.isUnknown) {
        feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the rollback opcode has unknown type", id.src, Error))
      }
    }
    super.rollback(res, resTypes, params, origin, stack)
  }
}
