package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.TypeParameterized
import samaya.structure.types.Type.{DefaultUnknown, Projected}
import samaya.structure.types._
import samaya.toolbox.track.TypeTracker
import samaya.validation.SignatureValidator

import scala.collection.immutable.ListMap

//Todo: Check no Unknown Types (only after join enough?)
trait TypeChecker extends TypeTracker{
  override def finalState(stack: Stack): Unit = {
    //check results
    for((v,r) <- stack.frameValues.zip(results.reverse)) {
      //check type
      val t = stack.getType(v)
      if (t != r.typ) {
        if(!t.isUnknown) {
          feedback(LocatedMessage(s"The value returned by the function has the wrong type: expected ${r.typ} got $t", component.fold(_.src,_.src), Error))
        }
      }
    }
    super.finalState(stack)
  }

  private lazy val parametrization:TypeParameterized = component match {
    case Left(value) => value
    case Right(value) => value
  }

  override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
    SignatureValidator.validateType(origin, res.typ, parametrization,context)
    res.typ match {
      //todo check that the type can be constructed
      //todo: is this sufficient for size check?
      case lit:LitType => if(lit.size(context) != value.bytes.length) {
        feedback(LocatedMessage("The number of literal bytes do not match the number of bytes expected by the type", origin, Error))
      }
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Can not create literal for a non-literal type", origin, Error))
      }
    }
    super.lit(res, value, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    SignatureValidator.validateType(origin, res.typ,parametrization,context)
    res.typ match {
      case adtType: AdtType =>
        val ctrs = adtType.ctrs(context)
        ctrs.get(ctr.name) match {
          case Some(value) =>
            val fields = value.values
            srcs.map(v => (v.src, stack.getType(v))).padTo(fields.size, (None, Type.DefaultUnknown)).zip(fields).foreach{
              case ((_, srcT), targT) if srcT == targT =>
              case ((id, srcT), targT) => if(!srcT.isUnknown && !targT.isUnknown) {
                feedback(LocatedMessage("The types of the supplied parameter do not match the type expected by the constructor", id.map(new InputSourceId(_)).getOrElse(origin), Error))
              }
            }
          case None => feedback(LocatedMessage("Matching constructor for pack not found", origin, Error))
        }
      case _ => feedback(LocatedMessage("Only internal data types can be packed", origin, Error))
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  private def checkFunctionCall(func: Func, params: Seq[Ref], stack: Stack, origin:SourceId):Unit = {
    val paramInfo = func.paramInfo(context)
    paramInfo.zip(params).foreach(tv => {
      val pType = stack.getType(tv._2)
      val loc = tv._2.src.map(new InputSourceId(_)).getOrElse(origin)
      if(pType != tv._1._1) {
        feedback(LocatedMessage(s"Function Parameter has the Wrong type: expected ${tv._1._1}, provided $pType", loc, Error))
      }
    })
    SignatureValidator.validateFunction(origin, func,parametrization,context)
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkFunctionCall(func, params, stack, origin)
    super.invoke(res, func, params, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkFunctionCall(sig, params, stack, origin)
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Only values with a signature type can be invoked", origin, Error))
      }
    }
    super.invokeSig(res, src, params, origin, stack)
  }

  override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    checkFunctionCall(func, params.map(_._2), stack, origin)
    super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
  }

  override def tryInvokeSigBefore(res: Seq[AttrId], src: Ref, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkFunctionCall(sig, params.map(_._2), stack, origin)
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Only values with a signature type can be invoked", origin, Error))
      }
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case _:Projected =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Only values of a projected type can be unprojected", origin, Error))
      }
    }
    super.unproject(res, src, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case _: AdtType =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Only internal data types can be unpacked", origin, Error))
      }
    }
    super.unpack(res, src, mode, origin, stack)
  }

  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case _: AdtType =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Only internal data types can be used as switch target", origin, Error))
      }
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case _: AdtType =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage("Only internal data types can be used as inspect target", origin, Error))
      }
    }
    super.inspectBefore(res, src, branches, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    resTypes.foreach(typ => SignatureValidator.validateType(origin, typ,parametrization,context))
    super.rollback(res, resTypes, params, origin, stack)
  }
}
