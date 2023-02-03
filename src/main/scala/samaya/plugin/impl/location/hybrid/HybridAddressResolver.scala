package samaya.plugin.impl.location.hybrid

import samaya.plugin.service.Selectors.SourceLookupMode
import samaya.plugin.service.{AddressResolver, Selectors}
import samaya.structure.ContentAddressable
import samaya.types.Address.{ContentBased, HybridAddress, LocationBased}
import samaya.types.{Address, Directory, Identifier, OutputTarget}
import scala.util.matching.Regex

class HybridAddressResolver extends AddressResolver{

  val Protocol:Regex = """^(.*)@(.*)$""".r

  override def matches(s: Selectors.AddressSelector): Boolean = s match {
    case Selectors.Lookup(directory, HybridAddress(target, loc), SourceLookupMode) =>
      AddressResolver.matches(Selectors.Lookup(directory, loc, SourceLookupMode))
    case Selectors.Parse(Protocol(hash, loc)) =>
      AddressResolver.matches(Selectors.Parse(hash)) &&
        AddressResolver.matches(Selectors.Parse(loc))
    case Selectors.SerializeAddress(parent, target, AddressResolver.Hybrid) =>
      AddressResolver.matches(Selectors.SerializeAddress(parent, target, AddressResolver.Location)) &&
        AddressResolver.matches(Selectors.SerializeAddress(parent, target, AddressResolver.Content))
    case _ => false
  }

  override def resolve[T <: ContentAddressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T], extensionFilter: Option[Set[String]]): Option[T] = path match {
    case HybridAddress(content, loc) => for(
      res <- AddressResolver.resolve(parent,loc, loader, extensionFilter)
      if res.hash == content.target
    ) yield res
    case _ => None
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

  override def resolveDirectory(parent: Directory, path: Address, create: Boolean): Option[Directory] = None
  override def resolveSink(parent: Directory, ident: Identifier.Specific): Option[OutputTarget] = None
  override def listSources(parent: Directory): Set[Identifier] = Set.empty
  override def listDirectories(parent: Directory): Set[Identifier] = Set.empty
  override def serializeDirectory(parent: Option[Directory], target: Directory): Option[String] = None
  override def provideDefault(): Option[Directory] = None
}


