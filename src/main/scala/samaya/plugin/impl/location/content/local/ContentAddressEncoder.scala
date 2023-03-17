package samaya.plugin.impl.location.content.local

import samaya.plugin.service.Selectors.SourceLookupMode
import samaya.plugin.service.{AddressResolver, ContentAddressResolver, Selectors}
import samaya.structure.types.Hash
import samaya.types.Address.ContentBased
import samaya.types.{Address, Addressable, ContentAddressable, Directory, Repository}

import scala.reflect.ClassTag

object ContentAddressEncoder {
  val Protocoll:String = "content"
  val prefix:String = AddressResolver.getProtocolHeader(Protocoll)
}

class ContentAddressEncoder extends ContentAddressResolver{
  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Parse(AddressResolver.protocol(ContentAddressEncoder.Protocoll, h)) => h.length == Hash.charLen
    case Selectors.SerializeAddress(_, _, AddressResolver.Content) => true
    case Selectors.Lookup(_, ContentBased(_), SourceLookupMode) => true
    //we do only parse, serialize and resolve
    case _ => false
  }

  override def resolve[T <: Addressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T]): Option[T] = {
    loader.asContentLoader match {
      case Some(contentLoader) => path match {
        case ContentBased(hash) => Repository.resolve(path, contentLoader) match {
          case Some(r) if r.hash == hash => Some(r)
          case _ => None
        }
        case _ => None
      }
      case None => None
    }

  }

  override def parsePath(ident: String): Option[Address] = ident match {
    case AddressResolver.protocol(ContentAddressEncoder.Protocoll, h) => Some(ContentBased(Hash.fromString(h)))
    case _ => None
  }

  override def serializeAddress(parent: Option[Directory], target: ContentAddressable): Option[String] = {
    Some(ContentAddressEncoder.prefix+target.hash)
  }

}
