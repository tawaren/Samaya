package samaya.plugin.impl.location.content.local

import samaya.plugin.impl.location.file.FileAddressResolver
import samaya.plugin.service.Selectors.SourceLookupMode
import samaya.plugin.service.{AddressResolver, ContentAddressResolver, Selectors}
import samaya.plugin.shared.index.CachedRepository
import samaya.plugin.shared.repositories.Repositories
import samaya.structure.{ContentAddressable, LinkablePackage}
import samaya.structure.types.Hash
import samaya.types.Address.{ContentBased, HybridAddress}
import samaya.types.{Address, Directory, Identifier, InputSource, OutputTarget}

import scala.reflect.ClassTag
import scala.util.matching.Regex

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

  override def resolve[T <: ContentAddressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T], extensionFilter: Option[Set[String]]): Option[T] = {
    implicit val classTag: ClassTag[T] = loader.tag
    val res = path match {
      case ContentBased(hash) =>
        CachedRepository.repo.get(hash) match {
          case Some(content : T) => Some(content)
          case _ => Repositories.active_repos.to(LazyList).flatMap(_.resolve(path, loader)).headOption
        }
      case _ => None
    }

    //todo: Integrate in lookup and continue looking if the extension is wrong
    (res, extensionFilter) match {
      case (Some(cont), Some(filter)) => cont.identifier.extension match {
        case Some(ext) if filter.contains(ext) => res
        case _ => None
      }
      case _ => res
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
