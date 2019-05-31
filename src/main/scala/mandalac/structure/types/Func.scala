package mandalac.structure.types

import mandalac.compilation.ErrorHandler._
import mandalac.registries.ModuleRegistry
import mandalac.structure
import mandalac.structure.Module

sealed trait Func {
  def paramInfo(current:Module):Seq[(Type,Boolean)]
  def returnInfo(current:Module):Seq[(Type,Set[Id])]
}

object Func {


  case class Local(offset:Int, applies:Seq[Type]) extends Func {
    def paramInfo(current:structure.Module): Seq[(Type, Boolean)] = {
        current.function(offset) match {
          case None =>
            feedback(SimpleMessage(s"Could not find function $offset in current module", Error))
            Seq.empty
          case Some(f) =>
            f.params.map(p => (p.typ.substitute(applies), p.consumes))
        }
    }

    def returnInfo(current:structure.Module): Seq[(Type, Set[Id])] = {
        current.function(offset) match {
          case None =>
            feedback(SimpleMessage(s"Could not find function $offset in current module with hash", Error))
            Seq.empty
          case Some(f) =>
            f.results.map(p => (p.typ.substitute(applies), p.borrows.map(b => Id(b))))
        }
    }
  }


  case class Module(module:Hash, offset:Int, applies:Seq[Type]) extends Func {
    def paramInfo(current:structure.Module): Seq[(Type, Boolean)] = {
      ModuleRegistry.moduleByHash(module) match {
        case None =>
          feedback(SimpleMessage(s"Could not find module with hash $module", Error))
          Seq.empty
        case Some(m) =>
          m.function(offset) match {
            case None =>
              feedback(SimpleMessage(s"Could not find function $offset in module with hash $module", Error))
              Seq.empty
            case Some(f) =>
              f.params.map(p => (p.typ.substitute(applies), p.consumes))
          }
      }
    }

    def returnInfo(current:structure.Module): Seq[(Type, Set[Id])] = {
      ModuleRegistry.moduleByHash(module) match {
        case None =>
          feedback(SimpleMessage(s"Could not find module with hash $module", Error))
          Seq.empty
        case Some(m) =>
          m.function(offset) match {
            case None =>
              feedback(SimpleMessage(s"Could not find function $offset in module with hash $module", Error))
              Seq.empty
            case Some(f) =>
              f.results.map(p => (p.typ.substitute(applies), p.borrows.map(b => Id(b))))
          }
      }
    }
  }

  case class Native(kind:NativeFun, applies:Seq[Type]) extends Func {
    override def paramInfo(current: structure.Module): Seq[(Type, Boolean)] = ???
    override def returnInfo(current: structure.Module): Seq[(Type, Set[Id])] = ???
  }

  sealed trait NativeFun {
    def ident:Int
  }

  object NativeFun {
    case object And extends NativeFun { override def ident: Int = 0 }
    case object Or extends NativeFun { override def ident: Int = 1 }
    case object Xor extends NativeFun { override def ident: Int = 2 }
    case object Not extends NativeFun { override def ident: Int = 3 }
    case object Extend extends NativeFun { override def ident: Int = 4 }
    case object Cut extends NativeFun { override def ident: Int = 5 }
    case object SignCast extends NativeFun { override def ident: Int = 6 }
    case object Add extends NativeFun { override def ident: Int = 7 }
    case object Sub extends NativeFun { override def ident: Int = 8 }
    case object Mul extends NativeFun { override def ident: Int = 9 }
    case object Div extends NativeFun { override def ident: Int = 10 }
    case object Eq extends NativeFun { override def ident: Int = 11 }
    case object Hash extends NativeFun { override def ident: Int = 12 }
    case object PlainHash extends NativeFun { override def ident: Int = 13 }
    case object Lt extends NativeFun { override def ident: Int = 14 }
    case object Gt extends NativeFun { override def ident: Int = 15 }
    case object Lte extends NativeFun { override def ident: Int = 16 }
    case object Gte extends NativeFun { override def ident: Int = 17 }
    case object ToData extends NativeFun { override def ident: Int = 18 }
    case object Concat extends NativeFun { override def ident: Int = 19 }
    case object SetBit extends NativeFun { override def ident: Int = 20 }
    case object GetBit extends NativeFun { override def ident: Int = 21 }
    case object GenPublicId extends NativeFun { override def ident: Int = 22 }
    case object DeriveId extends NativeFun { override def ident: Int = 23 }
  }
}

