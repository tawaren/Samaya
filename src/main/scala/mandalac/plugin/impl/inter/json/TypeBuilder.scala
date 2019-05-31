package mandalac.plugin.impl.inter.json

import mandalac.plugin.impl.inter.json.JsonModel.TypeEncodings
import mandalac.structure.Generic
import mandalac.structure.types.Type.NativeTypeKind
import mandalac.structure.types.{Hash, Type}

import scala.util.DynamicVariable


object TypeBuilder {

  //we do not want this to be passed around all over the place just for that
  val context:DynamicVariable[Seq[Generic]] = new DynamicVariable(Seq.empty)

  def inContext[T](caps:Seq[Generic])(body: => T): T = {
    context.withValue(caps)(body)
  }

  def toType(jtyp:JsonModel.Type):Type = {
    jtyp.datatype.module match {
      case TypeEncodings.Native() =>
        //todo: produce error if not their
        val offset = jtyp.datatype.offset.get
        NativeTypeKind.fromOffsetAndArg(offset, jtyp.datatype.args) match {
          case Some(kind) => Type.NativeType(kind, jtyp.applies.map(t => toType(t)))
          //Todo: shall we produce an error (is it expected or unexpected???)
          case None => Type.errorType
        }

      //todo: handle failure cases -> Unknown
      case TypeEncodings.Image() => Type.ImageType(toType(jtyp.applies.head))
      case TypeEncodings.Generic() =>
        //todo: produce error if not their
        val offset = jtyp.datatype.offset.get
        Type.GenericType(context.value(offset).capabilities,offset)
      case TypeEncodings.Local() =>
        //todo: produce error if not their
        val offset = jtyp.datatype.offset.get
        Type.LocalType(offset, jtyp.applies.map(t => toType(t)))
      case _ =>
        //todo: produce error if not their
        val offset = jtyp.datatype.offset.get
        Type.RealType(Hash.fromString(jtyp.datatype.module), offset, jtyp.applies.map(t => toType(t)))
    }
  }


}

