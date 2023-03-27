package samaya.plugin.impl.location.hybrid

import samaya.plugin.PluginProxy
import samaya.plugin.service.AddressResolver.SerializerMode
import samaya.plugin.service.Selectors.SourceLookupMode
import samaya.plugin.service.{AddressResolver, ContentAddressResolver, Selectors}
import samaya.types.Address.{ContentBased, HybridAddress, LocationBased}
import samaya.types.{Address, Addressable, ContentAddressable, Directory, Identifier, OutputTarget}

import scala.util.matching.Regex

class HybridAddressResolver extends ContentAddressResolver {

  val Protocol: Regex = """^(.*)@(.*)$""".r

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Lookup(HybridAddress(_, loc), SourceLookupMode) =>
      AddressResolver.matches(Selectors.Lookup(loc, SourceLookupMode))
    case Selectors.Parse(Protocol(hash, loc)) =>
      AddressResolver.matches(Selectors.Parse(hash)) &&
        AddressResolver.matches(Selectors.Parse(loc))
    //We do not check location modus instead if it will fail we just do the content
    case Selectors.SerializeAddress(target, AddressResolver.Hybrid(_)) =>
      AddressResolver.matches(Selectors.SerializeAddress(target, AddressResolver.Content))
    case _ => false
  }

  override def resolve[T <: Addressable](path: Address, loader: AddressResolver.Loader[T]): Option[T] = {
    loader.asContentLoader match {
      case Some(contentLoader) => path match {
        case HybridAddress(content, loc) => for (
          res <- AddressResolver.resolve(loc, contentLoader)
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

  override def serializeContentAddress(target: ContentAddressable, mode:SerializerMode): Option[String] = {
    val locationMode = mode match {
      case AddressResolver.Content => None
      case AddressResolver.Hybrid(locPart) => Some(locPart)
      case _ => return None
    }

    AddressResolver.serializeContentAddress(target, AddressResolver.Content) match {
      case Some(cont) => PluginProxy.resolveSilent(locationMode.flatMap(p => AddressResolver.serializeContentAddress(target, p))) match {
        case Some(loc) => Some(cont + "@" + loc) //We have a relative address for the location
        case None => Some(cont) //we do not have a relative address for the location
      }
      case None => None
    }
  }
}


