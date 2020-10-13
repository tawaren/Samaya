package samaya.structure.types


trait Location

object Location {
  case class FromFileStart(file:String, offset:Int) extends Location {
    override def toString: String = s"file $file offset $offset"
  }
  case class FromLineOffset(file:String, line:Int, coloumn:Int) extends Location {
    override def toString: String = s"file $file line $line:$coloumn"
  }
  case class Combined(file:String, line:Int, coloumn:Int, offset:Int) extends Location {
    override def toString: String = s"file $file line $line:$coloumn"
  }
  case object Unknown extends Location
}
