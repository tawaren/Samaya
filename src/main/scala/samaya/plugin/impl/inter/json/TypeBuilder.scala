package samaya.plugin.impl.inter.json

import samaya.plugin.impl.inter.json.JsonModel.{TypeEncodings, TypeKinds}
import samaya.structure.Generic
import samaya.structure.types.{AdtType, CompLink, Hash, LitType, SigType, Type}

import scala.util.DynamicVariable


object TypeBuilder {

  //we do not want this to be passed around all over the place just for that
  val context:DynamicVariable[Seq[Generic]] = new DynamicVariable(Seq.empty)

  def inContext[T](caps:Seq[Generic])(body: => T): T = {
    context.withValue(caps)(body)
  }

  def toType(jtyp:JsonModel.Type):Type = {

    //Todo: Add warnings?? when unknown parse (or even explizit error exept real unknown)
    jtyp.module match {
      case TypeEncodings.Generic() => jtyp.componentIndex match {
        case None => Type.DefaultUnknown
        case Some((_,offset)) => Type.GenericType(context.value(offset).capabilities, offset)
      }
      case TypeEncodings.Projection() => jtyp.applies.headOption match {
        case None => Type.DefaultUnknown
        case Some(innerType) => toType(innerType).projected()
      }
      case TypeEncodings.Unknown() => Type.DefaultUnknown
      case TypeEncodings.Local() => jtyp.componentIndex match {
        case Some((TypeKinds.Adt(), offset)) => AdtType.Local(offset, jtyp.applies.map(toType))
        case Some((TypeKinds.Lit(), offset)) => LitType.Local(offset, jtyp.applies.map(toType))
        case Some((TypeKinds.Sig(), offset)) => SigType.Local(offset, jtyp.applies.map(toType))
        case _ => Type.DefaultUnknown
      }

      case remote => jtyp.componentIndex match {
        case Some((TypeKinds.Adt(), offset)) => AdtType.Remote(CompLink.fromString(remote), offset, jtyp.applies.map(toType))
        case Some((TypeKinds.Lit(), offset)) => LitType.Remote(CompLink.fromString(remote), offset, jtyp.applies.map(toType))
        case Some((TypeKinds.Sig(), offset)) => SigType.Remote(CompLink.fromString(remote), offset, jtyp.applies.map(toType))
        case _ => Type.DefaultUnknown
      }
    }
  }


}

