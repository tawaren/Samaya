package samaya.plugin.impl.inter.json

import samaya.plugin.impl.inter.json.JsonModel.{TypeEncodings, TypeKinds}
import samaya.structure.Generic
import samaya.structure.types.{AdtType, CompLink, Hash, InputSourceId, LitType, Region, SigType, SourceId, Type}

import scala.util.DynamicVariable


object TypeBuilder {

  //we do not want this to be passed around all over the place just for that
  val context:DynamicVariable[Seq[Generic]] = new DynamicVariable(Seq.empty)

  def inContext[T](caps:Seq[Generic])(body: => T): T = {
    context.withValue(caps)(body)
  }

  def toType(jtyp:JsonModel.Type, loc:JsonLocation):Type = toType(jtyp, new InputSourceId(Region(loc,loc)))

  def toType(jtyp:JsonModel.Type, src:SourceId):Type = {
    val attrs = jtyp.attributes
    //Todo: Add warnings?? when unknown parse (or even explizit error exept real unknown)
    jtyp.module match {
      case TypeEncodings.Generic() => jtyp.componentIndex match {
        case None => Type.Unknown(Set.empty)(src, attrs)
        case Some((_,offset)) => Type.GenericType(context.value(offset).capabilities, offset)(src, attrs)
      }
      case TypeEncodings.Projection() => jtyp.applies.headOption match {
        case None => Type.Unknown(Set.empty)(src, attrs)
        case Some(innerType) => toType(innerType, src).projected(src, attrs)
      }
      case TypeEncodings.Unknown() => Type.Unknown(Set.empty)(src, attrs)
      case TypeEncodings.Local() => jtyp.componentIndex match {
        case Some((TypeKinds.Adt(), offset)) => AdtType.Local(offset, jtyp.applies.map(toType(_,src)))(src, attrs)
        case Some((TypeKinds.Lit(), offset)) => LitType.Local(offset, jtyp.applies.map(toType(_,src)))(src, attrs)
        case Some((TypeKinds.Sig(), offset)) => SigType.Local(offset, jtyp.applies.map(toType(_,src)))(src, attrs)
        case _ => Type.Unknown(Set.empty)(src)
      }

      case remote => jtyp.componentIndex match {
        case Some((TypeKinds.Adt(), offset)) => AdtType.Remote(CompLink.fromString(remote), offset, jtyp.applies.map(toType(_,src)))(src, attrs)
        case Some((TypeKinds.Lit(), offset)) => LitType.Remote(CompLink.fromString(remote), offset, jtyp.applies.map(toType(_,src)))(src, attrs)
        case Some((TypeKinds.Sig(), offset)) => SigType.Remote(CompLink.fromString(remote), offset, jtyp.applies.map(toType(_,src)))(src, attrs)
        case _ => Type.Unknown(Set.empty)(src, attrs)
      }
    }
  }
}

