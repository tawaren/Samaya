package mandalac.structure.types

import mandalac.registries.ModuleRegistry
import mandalac.compilation.ErrorHandler._
import mandalac.structure.{Generic, Module}

sealed trait Type {
  def ctrs:Seq[Seq[Type]] = ??? //todo:
  def capabilities(current:Module):Set[Capability]
  def hasCap(current:Module, cap: Capability):Boolean = capabilities(current).contains(cap)
  def isCurrentModule: Boolean = false
  def substitute(applies:Seq[Type]): Type = this
}

//todo: where are constraints checked??????
//todo: do we need an import validator
object Type {

  def restrictCapabilities(current:Module, base:Set[Capability], applies:Seq[(Generic,Type)]):Set[Capability] = {
    val rec = base.intersect(Capability.recursives)
    val none_rec = base -- Capability.recursives
    none_rec ++ applies.foldLeft(rec) { (r,gen) =>
      if(gen._1.phantom) {
        rec
      } else {
        rec.intersect(gen._2.capabilities(current))
      }
    }
  }

  case class LocalType(offset:Int, applies:Seq[Type]) extends Type {
    //todo: check how often we do call this and memorize if it is often
    override def capabilities(current:Module):Set[Capability] = {
        current.dataType(offset) match {
          case Some(dataType) => restrictCapabilities(current, dataType.capabilities, dataType.generics.zip(applies))
          case None =>
            feedback(SimpleMessage(s"Can not find dependent data type with index: $offset in current module", Error))
            Set.empty
        }
    }

    override def isCurrentModule = true
    override def substitute(applies: Seq[Type]): Type = LocalType(offset, this.applies.map(a => a.substitute(applies)))
  }


  case class RealType(moduleHash:Hash, offset:Int, applies:Seq[Type]) extends Type {
    //todo: check how often we do call this and memorize if it is often
    override def capabilities(current:Module):Set[Capability] = {
      ModuleRegistry.moduleByHash(moduleHash) match {
        case Some(module) =>
          module.dataType(offset) match {
            case Some(dataType) => restrictCapabilities(current, dataType.capabilities, dataType.generics.zip(applies))
            case None =>
              feedback(SimpleMessage(s"Can not find dependent data type with index: $offset in module with hash: $moduleHash", Error))
              Set.empty
          }
        case None =>
          feedback(SimpleMessage(s"Can not find dependent module with hash: $moduleHash", Error))
          Set.empty
      }
    }

    override def substitute(applies: Seq[Type]): Type = RealType(moduleHash,offset, this.applies.map(a => a.substitute(applies)))
  }

  case class NativeType(typ:NativeTypeKind, applies:Seq[Type]) extends Type{
    //todo: check how often we do call this and memorize if it is often
    override def capabilities(current:Module):Set[Capability] = restrictCapabilities(current,typ.capabilities, typ.generics.zip(applies))
    override def substitute(applies: Seq[Type]): Type = NativeType(typ, this.applies.map(a => a.substitute(applies)))
  }

  case class ImageType(typ:Type) extends Type {
    override def capabilities(current:Module):Set[Capability] = Capability.all
    override def substitute(applies: Seq[Type]): Type = ImageType(typ.substitute(applies))
  }
  case class GenericType(capabilities:Set[Capability], offset:Int) extends Type {
    override def capabilities(current: Module): Set[Capability] = capabilities
    override def substitute(applies: Seq[Type]): Type = {
      if(offset >= applies.size) {
        feedback(SimpleMessage(s"No instantiation for generic type provided during substitution", Error))
        Type.errorType
      } else {
        applies(offset)
      }
    }

  }

  sealed trait NativeTypeKind {
    def arg:Int = 0
    def ident:Int
    def capabilities:Set[Capability]
    def generics:Seq[Generic] = Seq.empty
  }


  object NativeTypeKind {
    private val opaqueCaps: Set[Capability] = Set(Capability.Embed, Capability.Drop, Capability.Copy, Capability.Persist)
    private val openCaps: Set[Capability] = Set(Capability.Consume, Capability.Inspect, Capability.Embed, Capability.Create, Capability.Drop, Capability.Copy, Capability.Persist)

    case class Data(override val arg:Int) extends NativeTypeKind {
      override val ident: Int = 0
      override def capabilities: Set[Capability] = opaqueCaps
    }

    case class SInt(override val arg:Int) extends NativeTypeKind {
      override val ident: Int = 1
      override def capabilities: Set[Capability] = opaqueCaps
    }

    case class UInt(override val arg:Int) extends NativeTypeKind {
      override val ident: Int = 2
      override def capabilities: Set[Capability] = opaqueCaps
    }

    case object Bool extends NativeTypeKind {
      override val ident: Int = 3
      override def capabilities: Set[Capability] = openCaps
    }

    case class Tuple(override val arg:Int) extends NativeTypeKind {
      override val ident: Int = 4
      override def capabilities: Set[Capability] = openCaps
      override def generics: Seq[Generic] = ??? //todo: not phantom & Not Protected & dummy Name
    }

    case class Alternative(override val arg:Int) extends NativeTypeKind {
      override val ident: Int = 5
      override def capabilities: Set[Capability] = openCaps
      override def generics: Seq[Generic] = ??? //todo: not phantom & Not Protected & dummy Name
    }

    case object PrivateId extends NativeTypeKind {
      override val ident: Int = 6
      override def capabilities: Set[Capability] = opaqueCaps
    }

    case object PublicId extends NativeTypeKind {
      override val ident: Int = 7
      override def capabilities: Set[Capability] = opaqueCaps
    }

    case object Nothing extends NativeTypeKind {
      override val ident: Int = 8
      override def capabilities: Set[Capability] = opaqueCaps
    }

    def fromOffsetAndArg(offset:Int, arg:Option[Int]): Option[NativeTypeKind] = {
      //todo: produce error if args missing
       offset match {
         case 0 => Some(Data(arg.get))
         case 1 => Some(SInt(arg.get))
         case 2 => Some(UInt(arg.get))
         case 3 => Some(Bool)
         case 4 => Some(Tuple(arg.get))
         case 5 => Some(Alternative(arg.get))
         case 6 => Some(PrivateId)
         case 7 => Some(PublicId)
         case 8 => Some(Nothing)
         case _ => None
       }
    }
  }

  val errorType:Type =  NativeType(NativeTypeKind.Nothing, Seq.empty)

}
