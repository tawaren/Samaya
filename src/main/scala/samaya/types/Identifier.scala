package samaya.types


sealed trait Identifier {
  def name:String
  def extension:Option[String]
  val fullName:String = extension.map(e => name + "." + e).getOrElse(name)

  override def toString: String = fullName
}

object Identifier {
  case class General(override val name:String) extends Identifier {
    override def extension: Option[String] = None
  }
  case class Specific(override val name:String, extensionString:Option[String]) extends Identifier {
    override def extension: Option[String] = extensionString
  }

  object Specific {
    def apply(name:String): Specific = {
      val res = name.split('.')
      if(res.length == 1) {
        Specific(res(0),None)
      } else {
        Specific(res(0),Some(name.drop(res(0).length+1)))
      }
    }
  }

  def apply(name:String): General = Identifier.General(name)
  def apply(name:String, extension:String): Specific = if(extension == "") {
    Identifier.Specific(name, None)
  } else {
    Identifier.Specific(name, Some(extension))
  }
}
