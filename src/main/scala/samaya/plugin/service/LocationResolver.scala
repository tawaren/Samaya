package samaya.plugin.service


import samaya.plugin.service.category.LocationResolverPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.types.{Identifier, InputSource, Location, OutputTarget, Path}

import scala.util.matching.Regex

//A plugin interface to resolve Locations
trait LocationResolver extends Plugin{

  override type Selector = Selectors.LocationSelector


  //generates a new location from a parent location and the sub location name
  def resolveLocation(parent:Location, path:Path):Option[Location]
  //finds a source in a location
  def resolveSource(parent:Location, path:Path, extensionFilter:Option[Set[String]] = None):Option[InputSource]
  //finds a output in a location
  def resolveSink(parent:Location, ident:Identifier.Specific):Option[OutputTarget]
  //list sources
  def listSources(parent:Location):Set[Identifier]
  //list sub locations
  def listLocations(parent:Location):Set[Identifier]
  //parse
  def parsePath(ident:String):Option[Path]
  //serialize
  def serialize(parent:Location, target:Location, resourceName:Option[String]):Option[String]
  //provide a default location
  def provideDefault():Option[Location]
}

object LocationResolver extends LocationResolver with PluginProxy{

  val protocol:Regex = """^(.*)://(.*)$""".r
  val pathSeparator:Regex = "[\\|/]".r
  def getProtocolHeader(protocolName:String):String = protocolName+"://"

  type PluginType = LocationResolver
  override def category: PluginCategory[PluginType] = LocationResolverPluginCategory

  def provideDefault():Option[Location] = {
    select(Selectors.Default).flatMap(r => r.provideDefault())
  }

  def resolveLocation(parent:Location, path:Path):Option[Location] = {
    select(Selectors.Lookup(parent, path) ).flatMap(r => r.resolveLocation(parent, path))
  }

  def resolveSource(parent:Location, path:Path, extensionFilter:Option[Set[String]] = None):Option[InputSource] = {
    select(Selectors.Lookup(parent, path) ).flatMap(r => r.resolveSource(parent, path, extensionFilter))
  }

  def resolveSink(parent:Location, ident:Identifier.Specific):Option[OutputTarget] = {
    select(Selectors.Lookup(parent, Path(ident)) ).flatMap(r => r.resolveSink(parent, ident))
  }

  def listSources(parent:Location):Set[Identifier] = {
    select(Selectors.List(parent)).map(r => r.listSources(parent)).getOrElse(Set.empty)
  }

  def listLocations(parent:Location):Set[Identifier] = {
    select(Selectors.List(parent)).map(r => r.listLocations(parent)).getOrElse(Set.empty)
  }

  override def parsePath(ident: String): Option[Path] = {
    select(Selectors.Parse(ident)).flatMap(r => r.parsePath(ident))
  }

  override def serialize(parent: Location, target: Location, resourceName:Option[String]): Option[String] = {
    select(Selectors.Serialize(parent,target)).flatMap(r => r.serialize(parent,target,resourceName))
  }
}
