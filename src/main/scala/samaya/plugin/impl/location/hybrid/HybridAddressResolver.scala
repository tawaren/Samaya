package samaya.plugin.impl.location.hybrid

import samaya.plugin.service.Selectors.SourceLookupMode
import samaya.plugin.service.{AddressResolver, ContentAddressResolver, Selectors}
import samaya.types.Address.{ContentBased, HybridAddress, LocationBased}
import samaya.types.{Address, Addressable, ContentAddressable, Directory, Identifier, OutputTarget}

import scala.util.matching.Regex

class HybridAddressResolver extends ContentAddressResolver {

  val Protocol: Regex = """^(.*)@(.*)$""".r

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Lookup(directory, HybridAddress(_, loc), SourceLookupMode) =>
      AddressResolver.matches(Selectors.Lookup(directory, loc, SourceLookupMode))
    case Selectors.Parse(Protocol(hash, loc)) =>
      AddressResolver.matches(Selectors.Parse(hash)) &&
        AddressResolver.matches(Selectors.Parse(loc))
    case Selectors.SerializeAddress(parent, target, AddressResolver.Hybrid) =>
      AddressResolver.matches(Selectors.SerializeAddress(parent, target, AddressResolver.Location)) &&
        AddressResolver.matches(Selectors.SerializeAddress(parent, target, AddressResolver.Content))
    case _ => false
  }

  override def resolve[T <: Addressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T]): Option[T] = {
    loader.asContentLoader match {
      case Some(contentLoader) => path match {
        case HybridAddress(content, loc) => for (
          res <- AddressResolver.resolve(parent, loc, contentLoader)
          if res.hash == content.target
        ) yield res
        case _ => None
      }
      case None => None
    }

  }
  override def parsePath(ident: String): Option[Address] = ident match {
    case Protocol(content, location) => for(
      target <- AddressResolver.parsePath(content);
      loc <- AddressResolver.parsePath(location)
      if target.isInstanceOf[ContentBased]
      if loc.isInstanceOf[LocationBased]
    ) yield HybridAddress(target.asInstanceOf[ContentBased], loc.asInstanceOf[LocationBased])
  }

  override def serializeAddress(parent: Option[Directory], target: ContentAddressable): Option[String] = {
    for (
      cont <- AddressResolver.serializeAddress(parent, target, AddressResolver.Content);
      loc <- AddressResolver.serializeAddress(parent, target, AddressResolver.Location)
    ) yield cont + "@" + loc
  }
}


