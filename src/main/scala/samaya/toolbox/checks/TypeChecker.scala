package samaya.toolbox.checks

import samaya.compilation.ErrorManager._
import samaya.structure.TypeParameterized
import samaya.structure.types.Type.Projected
import samaya.structure.types._
import samaya.toolbox.track.TypeTracker
import samaya.validation.SignatureValidator

import scala.collection.immutable.ListMap

//Todo: Check no Unknown Types (only after join enough?)
trait TypeChecker extends TypeTracker{

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
    for(((v,r), idx) <- stack.frameValues.zip(results.reverse).zipWithIndex) {
      //check type
      val src = entry.fold(_.src,_.src)
      val t = stack.getType(v)
      if (t != r.typ) {
        if(!t.isUnknown) {
          val modName = context.module.map(_.name+".").getOrElse("")
          val funName = context.pkg.name+"."+modName+name
          feedback(LocatedMessage(s"The ${indexToString(idx+1)} value returned by the function $funName has the wrong type: expected ${r.typ.prettyString(context,gens)} got ${t.prettyString(context,gens)}", src, Error))
        }
      }
    }
    super.finalState(stack)
  }

  private lazy val parametrization:TypeParameterized = entry match {
    case Left(value) => value
    case Right(value) => value
  }

  override def lit(res: TypedId, value: Const, origin: SourceId, stack: Stack): Stack = {
    SignatureValidator.validateType(origin, res.typ, parametrization,context)
    res.typ match {
      //todo check that the type can be constructed
      //todo: is this sufficient for size check?
      case lit:LitType => if(lit.size(context) != value.bytes.length) {
        feedback(LocatedMessage(s"Literals of type ${lit.prettyString(context, gens)} are ${lit.size(context)} bytes long but ${value.bytes.length} bytes where provided", origin, Error))
      }
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage(s"Type ${typ.prettyString(context, gens)} does not support literals ", origin, Error))
      }
    }
    super.lit(res, value, origin, stack)
  }

  override def pack(res: TypedId, srcs: Seq[Ref], ctr: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    SignatureValidator.validateType(origin, res.typ,parametrization,context)
    val fields = res.typ.projectionSeqMap{
      case adtType: AdtType =>
        val ctrs = adtType.ctrs(context)
        ctrs.get(ctr.name) match {
          case Some(value) => value.values.toSeq
          case None =>
            feedback(LocatedMessage(s"Type ${adtType.prettyString(context,gens)} does not have a constructor named ${ctr.name}", origin, Error))
            Seq.empty
        }
      case _ =>
        feedback(LocatedMessage(s"Type ${res.typ.prettyString(context,gens)} can not be constructed with a pack instruction", origin, Error))
        Seq.empty
    }

    srcs.map(v => (v.src, stack.getType(v))).padTo(fields.size, (UnknownSourceId, Type.Unknown(Set.empty)(origin))).zip(fields).zipWithIndex.foreach{
      case (((_, srcT), targT),_) if srcT == targT =>
      case (((src, srcT), targT), idx) => if(!srcT.isUnknown && !targT.isUnknown) {
        feedback(LocatedMessage(s"${indexToString(idx+1)} field value for constructor call ${res.typ.prettyString(context,gens)}#${ctr.name} has the wrong type: expected ${targT.prettyString(context, gens)}, provided ${srcT.prettyString(context, gens)}", src, Error))
      }
    }
    super.pack(res, srcs, ctr, mode, origin, stack)
  }

  private def checkFunctionCall(func: Func, params: Seq[Ref], stack: Stack, origin:SourceId):Unit = {
    val paramInfo = func.paramInfo(context)
    paramInfo.zip(params).zipWithIndex.foreach{case (tv,idx) => {
      val pType = stack.getType(tv._2)
      val loc = tv._2.src
      if(pType != tv._1._1) {
        feedback(LocatedMessage(s"${indexToString(idx+1)} parameter value for function call ${func.prettyString(context,gens)} has the wrong type: expected ${tv._1._1.prettyString(context, gens)}, provided ${pType.prettyString(context, gens)}", loc, Error))
      }
    }}
    SignatureValidator.validateFunction(func,parametrization,context)
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    checkFunctionCall(func, params, stack, origin)
    super.invoke(res, func, params, origin, stack)
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case sig:SigType => checkFunctionCall(sig, params, stack, origin)
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage(s"Values of type ${typ.prettyString(context,gens)} can not be invoked (only values of a signature type can be invoked)", origin, Error))
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
        feedback(LocatedMessage(s"Values of type ${typ.prettyString(context,gens)} can not be invoked (only values of a signature type can be invoked)", origin, Error))
      }
    }
    super.tryInvokeSigBefore(res, src, params, succ, fail, origin, stack)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src) match {
      case _:Projected =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage(s"Values of type ${typ.prettyString(context,gens)} can not be unprojected (only values of a projected type can be unprojected)", origin, Error))
      }
    }
    super.unproject(res, src, origin, stack)
  }

  override def unpack(res: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract {
      case _: AdtType =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage(s"Type ${typ.prettyString(context,gens)} can not be deconstructed with a unpack instruction", origin, Error))
      }
    }
    super.unpack(res, src, mode, origin, stack)
  }


  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract {
      case _: AdtType =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage(s"Type ${typ.prettyString(context,gens)} can not be deconstructed with a field instruction", origin, Error))
      }
    }
    super.field(res, src, fieldName, mode, origin, stack)
  }

  override def switchBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract {
      case _: AdtType =>
      case typ => if(!typ.isUnknown){
        feedback(LocatedMessage(s"Type ${typ.prettyString(context,gens)} can not be deconstructed with a switch instruction", origin, Error))
      }
    }
    super.switchBefore(res, src, branches, mode, origin, stack)
  }

  override def inspectBefore(res: Seq[AttrId], src: Ref, branches: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Stack = {
    stack.getType(src).projectionExtract{
      case _: AdtType =>
      case typ => if(!typ.isUnknown) {
        feedback(LocatedMessage(s"Type ${typ.prettyString(context,gens)} can not be inspected", origin, Error))
      }
    }
    super.inspectBefore(res, src, branches, origin, stack)
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    resTypes.foreach(typ => SignatureValidator.validateType(origin, typ,parametrization,context))
    super.rollback(res, resTypes, params, origin, stack)
  }
}
