package samaya.plugin.impl.location.relative

import samaya.plugin.service.{AddressResolver, Selectors}
import samaya.structure.ContentAddressable
import samaya.types.{Address, Identifier, InputSource, Directory, OutputTarget}

class RelativeAddressParser extends AddressResolver{

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
    val pathIds = parts.map(elem => {
      if(elem == "..") {
        Identifier.General("..")
      } else {
        val nameExt = elem.split('.')
        if (nameExt.length > 1) {
          Identifier.Specific(nameExt(0), parts.last.drop(nameExt(0).length + 1))
        } else {
          Identifier.General(nameExt(0))
        }
      }
    })
    Some(Address.Relative(pathIds))
  }



  //Not supported by this plugin on purpose
  override def resolveDirectory(parent: Directory, path: Address, create:Boolean): Option[Directory] = None
  override def resolve[T <: ContentAddressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T], extensionFilter:Option[Set[String]] = None): Option[T] = None
  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = None
  override def listSources(parent: Directory): Set[Identifier] = Set.empty
  override def listDirectories(parent: Directory): Set[Identifier] = Set.empty
  override def provideDefault(): Option[Directory] = None
  override def serializeAddress(parent: Option[Directory], target: ContentAddressable): Option[String] = None
  override def serializeDirectory(parent: Option[Directory], target: Directory): Option[String] = None
}
