package samaya.types

import samaya.plugin.service.AddressResolver


sealed trait Identifier {
  def name:String
  def extension:Option[String]
  def fullName:String
  def partialName:String = fullName
  override def toString: String = fullName
}

object Identifier {

  val wildcard:String = ".*";

  case class General(override val name:String, extensionString:Option[String]) extends Identifier {
    val fullName:String = extension.map(e => name + "." + e + wildcard).getOrElse(name)
    override val partialName: String = extension.map(e => name + "." + e).getOrElse(name)
    override def extension: Option[String] = extensionString
  }
  case class Specific(override val name:String, extensionString:Option[String]) extends Identifier {
    val fullName:String = extension.map(e => name + "." + e).getOrElse(name)
    override def extension: Option[String] = extensionString
  }

  object Specific {
    def apply(name:String): Specific = {
      //We treat names with only .'s special to support relative paths
      if(name == ".." | name == ".") {
        Specific(name,None)
      } else {
        val res = name.split('.')
        if(res.length == 1) {
          Specific(res(0),None)
        } else {
          Specific(res(0),Some(name.drop(res(0).length+1)))
        }
      }
    }
    def apply(name:String, extension:String): Specific = Specific(name, Some(extension))
  }

  def apply(name:String, extension:String): Identifier = if(extension == "") {
    Identifier.Specific(name, None)
  } else if(extension.endsWith(wildcard)) {
    Identifier.General(name, Some(extension.dropRight(wildcard.length)))
  } else{
    Identifier.Specific(name, Some(extension))
  }

  def apply(name:String): Identifier = {
    if(name.contains(".")){
      val res = name.split("\\.",  2)
      Identifier(res(0), res(1))
      //Todo: I do not like this here as it treats pathSeparator independent of concrete AddressResolver
    } else if(AddressResolver.pathSeparator.matches(""+name.last)){
      Identifier.Specific(name, None)
    } else {
      Identifier.General(name, None)
    }
  }
}
