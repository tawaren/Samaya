package mandalac.plugin.service

import java.io.OutputStream
import java.util.regex.Pattern

import mandalac.plugin.service.category.LocationResolverPluginCategory
import mandalac.plugin.{Plugin, PluginProxy}
import mandalac.types.{Identifier, InputSource, Location, Path}

import scala.util.matching.Regex

//A plugin interface to resolve Locations
trait LocationResolver extends Plugin{

  override type Selector = Selectors.LocationSelector


  //generates a new location from a parent location and the sub location name
  def resolveLocation(parent:Location, path:Path):Option[Location]
  //finds a source in a location
  def resolveSource(parent:Location, path:Path):Option[InputSource]
  //finds a output in a location
  def resolveSink(parent:Location, ident:Identifier.Specific):Option[OutputStream]
  //list sources
  def listSources(parent:Location):Set[Identifier]
  //list sub locations
  def listLocations(parent:Location):Set[Identifier]
  //parse
  def parsePath(ident:String):Option[Path]
  //serialize
  def serializeLocation(parent:Location, target:Location):Option[String]
  //provide a default location
  def provideDefault():Option[Location]
}

object LocationResolver extends LocationResolver with PluginProxy{

  val protocol:Regex = """^(.*)://(.*)$""".r
  val pathSeparator:Regex = "\\|/".r
  def getProtocolHeader(protocolName:String):String = protocolName+"://"

  type PluginType = LocationResolver
  override def category: PluginCategory[PluginType] = LocationResolverPluginCategory

  def provideDefault():Option[Location] = {
    select(Selectors.Default).flatMap(r => r.provideDefault())
  }

  def resolveLocation(parent:Location, path:Path):Option[Location] = {
    select(Selectors.Lookup(parent, path) ).flatMap(r => r.resolveLocation(parent, path))
  }

  def resolveSource(parent:Location, path:Path):Option[InputSource] = {
    select(Selectors.Lookup(parent, path) ).flatMap(r => r.resolveSource(parent, path))
  }

  def resolveSink(parent:Location, ident:Identifier.Specific):Option[OutputStream] = {
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

  override def serializeLocation(parent: Location, target: Location): Option[String] = {
    select(Selectors.Serialize(parent,target)).flatMap(r => r.serializeLocation(parent,target))
  }
}
