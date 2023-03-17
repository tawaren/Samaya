package samaya.plugin.impl.location.relative

import samaya.plugin.service.{AddressResolver, ReadOnlyAddressResolver, Selectors}
import samaya.types.{Address, Addressable, ContentAddressable, Directory, Identifier}

class RelativeAddressParser extends ReadOnlyAddressResolver{

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    //we can not parse absolutes for a specific protocoll
    case Selectors.Parse(AddressResolver.protocol(_ , _)) => false
    //we can parse non absolutes (not for a protocoll)
    case Selectors.Parse(_) => true
    //we do only parse
    case _ => false
  }

  override def parsePath(path: String): Option[Address] =  {
    val parts = AddressResolver.pathSeparator.split(path)
    val pathIds = parts.init.map(elem => Identifier.Specific(elem))
    val nameExt = parts.last.split('.')
    val lastId = if(nameExt.length > 1) {
      Identifier.Specific(parts.last)
    } else if(AddressResolver.pathSeparator.matches(""+path.last)){
      Identifier.Specific(nameExt(0), None)
    } else {
      Identifier.General(nameExt(0))
    }
    Some(Address.Relative(pathIds :+ lastId))
  }

  //Not supported by this plugin on purpose
  override def resolveDirectory(parent: Directory, path: Address, create:Boolean): Option[Directory] = None
  override def resolve[T <: Addressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T]): Option[T] = None
  override def listSources(parent: Directory): Set[Identifier] = Set.empty
  override def listDirectories(parent: Directory): Set[Identifier] = Set.empty
  override def provideDefault(): Option[Directory] = None
  override def serializeAddress(parent: Option[Directory], target: ContentAddressable): Option[String] = None
  override def serializeDirectory(parent: Option[Directory], target: Directory): Option[String] = None
}
