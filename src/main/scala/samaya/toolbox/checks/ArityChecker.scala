package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.types.{AdtType, AttrId, FetchMode, Func, Id, OpCode, Ref, SigType, SourceId, Type, TypedId}
import samaya.toolbox.track.TypeTracker

//todo: split into Untyped & Typed Arity Checker
trait ArityChecker extends TypeTracker{

  private val gens = entry match {
    case Left(value) => value.generics.map(_.name)
    case Right(value) => value.generics.map(_.name)
  }

  override def traverseBlockStart(input: Seq[AttrId], result: Seq[Id], code: Seq[OpCode], origin: SourceId, stack: Stack): Stack = {
    val bodyRes = code.filter(!_.isVirtual).last.rets.size;
    if(code.filter(!_.isVirtual).last.rets.size != result.size) {
      feedback(LocatedMessage(s"The block body returns $bodyRes values but ${result.size} values were expected", origin, Error))
    }
    super.traverseBlockStart(input, result, code, origin, stack)
  }

  override def finalState(stack: Stack): Unit = {
    if(stack.stackSize() != results.size) {
      val src = entry.fold(_.src,_.src)
      val modName = context.module.map(_.name+".").getOrElse("")
      val funName = context.pkg.name+"."+modName+name
      feedback(LocatedMessage(s"The body of function $funName returns ${stack.stackSize()} values but ${results.size} values were expected", src, Error))
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
            feedback(LocatedMessage(s"The case extracts ${fields.size} values but the constructor ${adt.prettyString(context,gens)}#${ctr.name} only defines ${args.size} fields", origin, Error))
          }
          case None => feedback(LocatedMessage(s"Constructor ${adt.prettyString(context,gens)}#${ctr.name} does not exist", ctr.src, Error))
        }
      case _ =>
    }
    super.caseStart(fields, src, ctr, mode, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt:AdtType =>
        val ctrs = adt.ctrs(context)
        if(ctrs.size != 1) {
          feedback(LocatedMessage(s"${adt.prettyString(context,gens)} has ${ctrs.size} constructors but 1 was expected", origin, Error))
        }
        ctrs.headOption match {
          //check that whe have the right amount of fields
          case Some((name,fields)) => if(fields.size != res.size) {
            feedback(LocatedMessage(s"The unpack extracts ${res.size} values but the constructor ${adt.prettyString(context,gens)}#$name only defines ${fields.size} fields", origin, Error))
          }
          case _ =>
        }
      case _ =>
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case adt:AdtType =>
        val ctrs = adt.ctrs(context)
        if(ctrs.size != 1) {
          feedback(LocatedMessage(s"${adt.prettyString(context,gens)} has ${ctrs.size} constructors but 1 was expected", origin, Error))
        }
        ctrs.headOption match {
          //check that whe have the right amount of fields
          case Some((name,fields)) => if(!fields.contains(fieldName.name)) {
            feedback(LocatedMessage(s"The field ${fieldName.name} is not a field defined by the constructor ${adt.prettyString(context,gens)}#$name", fieldName.src, Error))
          }
          case _ =>
        }
      case _ =>
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin:SourceId, stack: Stack):Stack = {
    res.typ match {
      case adt: AdtType => if(!adt.ctrs(context).contains(ctr.name)) {
          feedback(LocatedMessage(s"Constructor ${adt.prettyString(context,gens)}#${ctr.name} does not exist", ctr.src, Error))
      }
      case _ =>
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  def checkInvoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin:SourceId): Unit = {
    val paramInfo = func.paramInfo(context)
    if(paramInfo.size != params.size) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"The function ${func.prettyString(context,gens)} takes ${paramInfo.size} arguments but ${params.size} were provided", origin, Error))
      }
    }

    val retInfo = func.returnInfo(context)
    if(retInfo.size != res.size) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"The function ${func.prettyString(context,gens)} returns ${retInfo.size} values but ${res.size} were expected", origin, Error))
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
        feedback(LocatedMessage(s"The function ${func.prettyString(context,gens)} takes ${paramInfo.size} arguments but ${params.size} were provided", origin, Error))
      }
    }
    val essentials = params.count(b => b)
    if(essentials != failParams) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"The call to ${func.prettyString(context,gens)} has $essentials arguments but $failParams were expected by the fail case", origin, Error))
      }
    }

    val retInfo = func.returnInfo(context)
    if(retInfo.size != succParams) {
      if(!func.isUnknown) {
        feedback(LocatedMessage(s"The function ${func.prettyString(context,gens)} returns ${retInfo.size} values but $succParams were expected by the success case", origin, Error))
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
      feedback(LocatedMessage(s"The rollback defines ${resTypes.size} return types but expects ${res.size} returned values", origin, Error))
    }
    super.rollback(res, resTypes, params, origin, stack)
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    if(res.size != src.size) {
      feedback(LocatedMessage(s"The return opcode defines ${src.size} arguments but expects ${res.size} returned values", origin, Error))
    }
    super._return(res, src, origin, stack)
  }
}
