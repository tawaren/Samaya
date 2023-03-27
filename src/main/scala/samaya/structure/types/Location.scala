package samaya.structure.types

import samaya.types.ContentAddressable


trait Location {
  def localRefString: String
}

object Location {
  case class FromFileStart(src:ContentAddressable, offset:Int) extends Location {
    override def toString: String = s"${src.identifier.fullName} @ $localRefString - ${src.location}"
    override def localRefString: String = s"$offset"
  }
  case class FromLineOffset(src:ContentAddressable, line:Int, coloumn:Int) extends Location {
    override def toString: String = s"${src.identifier.fullName} @ $localRefString - ${src.location}"
    override def localRefString: String = s"$line:$coloumn"
  }
  case class Combined(src:ContentAddressable, line:Int, coloumn:Int, offset:Int) extends Location {
    override def toString: String = s"${src.identifier.fullName} @ $localRefString - ${src.location}"
    override def localRefString: String = s"$line:$coloumn"
  }
  case object Unknown extends Location {
    override def localRefString: String = "unknown"
  }
}
