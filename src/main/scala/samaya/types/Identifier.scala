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
  case class Specific(override val name:String, extensionString:String) extends Identifier {
    override def extension: Option[String] = Some(extensionString)
  }
  def apply(name:String): General = Identifier.General(name)
  def apply(name:String, extension:String): Specific = Identifier.Specific(name, extension)
}
