package mandalac.structure.types



sealed trait Risk

object Risk {
  case class Local(offset:Int) extends Risk
  case class Module(module:Hash, offset:Int) extends Risk
  case class Native(kind:NativeRisk) extends Risk

  sealed trait NativeRisk {
    def ident:Int
  }

  object NativeRisk {
    case object NumericError extends NativeRisk { override def ident: Int = 0 }
    case object IndexError extends NativeRisk { override def ident: Int = 1 }
    case object Unexpected extends NativeRisk { override def ident: Int = 2 }

    def fromIdent(ident: Int):Option[NativeRisk] = {
      ident match {
        case 0 => Some(NumericError)
        case 1 => Some(IndexError)
        case 2 => Some(Unexpected)
        case _ => None
      }
    }
  }

}
