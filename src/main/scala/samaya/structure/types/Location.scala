package samaya.structure.types


trait Location {
  def localRefString: String
}

object Location {
  case class FromFileStart(file:String, offset:Int) extends Location {
    override def toString: String = s"file $file $localRefString"
    override def localRefString: String = s"offset $offset"
  }
  case class FromLineOffset(file:String, line:Int, coloumn:Int) extends Location {
    override def toString: String = s"file $file $localRefString"
    override def localRefString: String = s"line $line:$coloumn"
  }
  case class Combined(file:String, line:Int, coloumn:Int, offset:Int) extends Location {
    override def toString: String = s"file $file $localRefString"
    override def localRefString: String = s"line $line:$coloumn"
  }
  case object Unknown extends Location {
    override def localRefString: String = "unknown"
  }
}
