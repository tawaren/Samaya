package samaya.plugin.impl.location.content.local

import samaya.plugin.impl.location.file.FileAddressResolver
import samaya.plugin.service.{AddressResolver, Selectors}
import samaya.structure.ContentAddressable
import samaya.structure.types.Hash
import samaya.types.Address.ContentBased
import samaya.types.{Address, Directory, Identifier, OutputTarget}

import scala.util.matching.Regex

object ContentAddressEncoder {
  val Protocoll:String = "content"
  val prefix:String = AddressResolver.getProtocolHeader(Protocoll)
}

class ContentAddressEncoder extends AddressResolver{
  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Parse(AddressResolver.protocol(ContentAddressEncoder.Protocoll, h)) => h.length == Hash.charLen
    case Selectors.SerializeAddress(_, _, AddressResolver.Content) => true
    //we do only parse and serialize
    case _ => false
  }

  override def parsePath(ident: String): Option[Address] = ident match {
    case AddressResolver.protocol(ContentAddressEncoder.Protocoll, h) => Some(ContentBased(Hash.fromString(h)))
    case _ => None
  }

  override def serializeAddress(parent: Option[Directory], target: ContentAddressable): Option[String] = {
    Some(ContentAddressEncoder.prefix+target.hash)
  }


  //Todo: Implement using Indexer
  override def resolveDirectory(parent: Directory, path: Address, create: Boolean): Option[Directory] = None
  override def resolve[T <: ContentAddressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T], extensionFilter: Option[Set[String]]): Option[T] = None
  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = None
  override def listSources(parent: Directory): Set[Identifier] = Set.empty
  override def listDirectories(parent: Directory): Set[Identifier] = Set.empty
  override def serializeDirectory(parent: Option[Directory], target: Directory): Option[String] = None
  override def provideDefault(): Option[Directory] = None
}
