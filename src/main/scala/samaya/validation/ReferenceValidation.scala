package samaya.validation

import samaya.compilation.ErrorManager.{Checking, Error, LocatedMessage, feedback}
import samaya.config.Config
import samaya.structure.{DataDef, ModuleEntry, TypeParameterized}
import samaya.structure.types.{AdtType, DefinedType, SourceId, Type}
import samaya.types.Context

object ReferenceValidation {
  private def validateTypeReference(src:SourceId, t:Type, entry:ModuleEntry, context:Context): Unit ={
    t.projectionExtract {
      case adt:AdtType if adt.isCurrentModule && Config.forwardRef.value => adt.getEntry(context) match {
        case Some(adtDef) if adtDef.position < entry.position =>
        case Some(_) => feedback(LocatedMessage("DataTypes can not contain forward references to other data types unless used for a phantom type parameter", src, Error, Checking()))
        case None => feedback(LocatedMessage("Requested DataType does not exist", src, Error, Checking()))
      }
      case typ : DefinedType[ModuleEntry] if typ.isCurrentModule && !Config.forwardRef.value => typ.getEntry(context) match {
        case Some(typDef) if typDef.position < entry.position =>
        case Some(_) => feedback(LocatedMessage("Types can not contain forward references", src, Error, Checking()))
        case None => feedback(LocatedMessage("Requested Type does not exist", src, Error, Checking()))
      }
      case _ =>
    }
  }

  private def validateTypeReferences(t:Type, entry: ModuleEntry, context:Context):Unit = {
    t.projectionExtract {
      case adt:AdtType if Config.forwardRef.value => adt.getEntry(context) match {
        case Some(adtDef) => adt.applies.zip(adtDef.generics).filterNot(_._2.phantom).foreach{
          case (t , _) => validateTypeReference(t.src,t, entry,context)
        }
        case _ =>
      }
      case typ : DefinedType[ModuleEntry] if !Config.forwardRef.value => typ.getEntry(context) match {
        case Some(typDef:TypeParameterized) => typ.applies.zip(typDef.generics).filterNot(_._2.phantom).foreach{
          case (t , _) => validateTypeReference(t.src,t, entry,context)
        }
        case _ =>
      }
      case _ =>
    }
    t.applies.foreach(t => validateTypeReferences(t, entry, context))
  }

  def validateReferences(t:Type, entry:ModuleEntry, context:Context):Unit = {
    validateTypeReference(t.src, t, entry, context)
    validateTypeReferences(t, entry,context)
  }
}
