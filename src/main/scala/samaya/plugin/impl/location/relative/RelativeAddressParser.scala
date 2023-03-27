package samaya.plugin.impl.location.relative

import samaya.plugin.service.AddressResolver.DirectoryMode
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
    val parts = AddressResolver.pathSeparator.split(path).filter(_.nonEmpty)
    if(parts.isEmpty) return Some(Address.Relative(Seq.empty))
    val pathIds : Seq[Identifier] = parts.init.toIndexedSeq.map(elem => Identifier.Specific(elem))
    val lastId = Identifier(parts.last)
    Some(Address.Relative(pathIds :+ lastId))
  }

  //Not supported by this plugin on purpose
  override def resolveDirectory(path: Address, mode:DirectoryMode): Option[Directory] = None
  override def list(parent: Directory, filter: Option[AddressResolver.AddressKind]): Set[Identifier] = Set.empty
  override def resolve[T <: Addressable](path: Address, loader: AddressResolver.Loader[T]): Option[T] = None
  override def provideDefault(): Option[Directory] = None
  override def serializeContentAddress(target: ContentAddressable, mode: AddressResolver.SerializerMode): Option[String] = None
  override def serializeDirectoryAddress(target: Directory, mode: AddressResolver.SerializerMode): Option[String] = None
}
