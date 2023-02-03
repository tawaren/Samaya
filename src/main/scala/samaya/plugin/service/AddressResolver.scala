package samaya.plugin.service


import samaya.plugin.service.AddressResolver.SerializerMode
import samaya.plugin.service.category.AddressResolverPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.ContentAddressable
import samaya.structure.types.Hash
import samaya.types.{Address, Directory, Identifier, InputSource, OutputTarget}

import scala.util.matching.Regex

//A plugin interface to resolve Locations
trait AddressResolver extends Plugin{

  override type Selector = Selectors.AddressSelector

  //generates a new location from a parent location and the sub location name
  def resolveDirectory(parent:Directory, path:Address, create:Boolean = false):Option[Directory]
  //finds a source
  def resolve[T <: ContentAddressable](parent:Directory, path:Address, loader:AddressResolver.Loader[T], extensionFilter:Option[Set[String]] = None):Option[T]
  //finds a sink
  def resolveSink(parent:Directory, ident:Identifier.Specific):Option[OutputTarget]
  //list sources
  def listSources(parent:Directory):Set[Identifier]
  //list sub locations
  def listDirectories(parent:Directory):Set[Identifier]
  //parse
  def parsePath(ident:String):Option[Address]
  //serialize
  def serializeAddress(parent:Option[Directory], target:ContentAddressable):Option[String]
  def serializeDirectory(parent:Option[Directory], target: Directory): Option[String]
    //provide a default location
  def provideDefault():Option[Directory]
}

object AddressResolver extends AddressResolver with PluginProxy{

  sealed trait SerializerMode
  case object Content extends SerializerMode
  case object Location extends SerializerMode
  case object Hybrid extends SerializerMode

  trait Loader[T <: ContentAddressable] {
    def load(src:InputSource):Option[T]
    def hash(trg:T):Hash = trg.hash
  }

  object InputLoader extends Loader[InputSource] {
    override def load(src: InputSource): Option[InputSource] = Some(src)
  }

  val protocol:Regex = """^(.*)://(.*)$""".r
  val pathSeparator:Regex = "[\\|/]".r
  def getProtocolHeader(protocolName:String):String = protocolName+"://"

  type PluginType = AddressResolver
  override def category: PluginCategory[PluginType] = AddressResolverPluginCategory

  def provideDefault():Option[Directory] = {
    select(Selectors.Default).flatMap(r => r.provideDefault())
  }

  def resolveDirectory(parent:Directory, path:Address, create:Boolean = false):Option[Directory] = {
    select(Selectors.Lookup(parent, path, Selectors.LocationLookupMode) ).flatMap(r => r.resolveDirectory(parent, path, create))
  }

  def resolve[T <: ContentAddressable](parent:Directory, path:Address, loader:Loader[T], extensionFilter:Option[Set[String]] = None):Option[T] = {
    select(Selectors.Lookup(parent, path, Selectors.SourceLookupMode) ).flatMap(r => r.resolve(parent, path, loader, extensionFilter))
  }

  def resolveSink(parent:Directory, ident:Identifier.Specific):Option[OutputTarget] = {
    select(Selectors.Lookup(parent, Address(ident), Selectors.SinkLookupMode) ).flatMap(r => r.resolveSink(parent, ident))
  }

  def listSources(parent:Directory):Set[Identifier] = {
    select(Selectors.List(parent)).map(r => r.listSources(parent)).getOrElse(Set.empty)
  }

  def listDirectories(parent:Directory):Set[Identifier] = {
    select(Selectors.List(parent)).map(r => r.listDirectories(parent)).getOrElse(Set.empty)
  }

  override def parsePath(ident: String): Option[Address] = {
    select(Selectors.Parse(ident)).flatMap(r => r.parsePath(ident))
  }

  override def serializeDirectory(parent:Option[Directory], target: Directory): Option[String] = {
    select(Selectors.SerializeDirectory(parent,target)).flatMap(r => r.serializeDirectory(parent,target))
  }

  def serializeAddress(parent:Option[Directory], target: ContentAddressable, mode:SerializerMode): Option[String] = {
    select(Selectors.SerializeAddress(parent, target, mode)).flatMap(r => r.serializeAddress(parent, target))
  }

  override def serializeAddress(parent:Option[Directory], target: ContentAddressable): Option[String] = {
    serializeAddress(parent, target, Hybrid)
  }
}
