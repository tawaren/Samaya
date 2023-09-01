package samaya.structure.types

sealed trait CompLink

object CompLink {
  private val CODE_PREFIX = "code:"
  private val INTERFACE_PREFIX = "interface:"

  case class ByCode(hash:Hash) extends CompLink {
    override def toString: String = CODE_PREFIX+hash
  }

  case class ByInterface(hash:Hash) extends CompLink {
    override def toString: String = INTERFACE_PREFIX+hash
  }

  def fromString(str:String):CompLink = {
    if(str.startsWith(CODE_PREFIX)) {
      ByCode(Hash.fromString(str.substring(CODE_PREFIX.length)))
    } else if(str.startsWith(INTERFACE_PREFIX)) {
      ByInterface(Hash.fromString(str.substring(INTERFACE_PREFIX.length)))
    } else {
      //Assume code ref
      ByCode(Hash.fromString(str))
    }
  }

}
