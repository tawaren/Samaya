package samaya.plugin.service


import samaya.plugin.service.AddressResolver.SerializerMode
import samaya.plugin.service.category.AddressResolverPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.types.Hash
import samaya.types.{Address, Addressable, ContentAddressable, Directory, GeneralSource, Identifier, InputSource, OutputTarget}

import scala.reflect.ClassTag
import scala.util.matching.Regex

//A plugin interface to resolve Content & Location Addresses
trait AddressResolver extends Plugin{

  override type Selector = Selectors.AddressSelector

  //generates a new location from a parent location and the sub location name
  def resolveDirectory(parent:Directory, path:Address, create:Boolean = false):Option[Directory]
  //dletes a existing location from a parent location and the sub location name
  def deleteDirectory(dir:Directory):Unit
  //finds a source
  def resolve[T <: Addressable](parent: Directory, path: Address, loader: AddressResolver.Loader[T]):Option[T]
  //finds a sink
  def resolveSink(parent:Directory, ident:Identifier.Specific):Option[OutputTarget]
  //Todo: a DeleteSink
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

  trait Loader[T <: Addressable] {
    def asContentLoader : Option[ContentLoader[T with ContentAddressable]] = None
    def load(src:GeneralSource):Option[T]
    def tag:ClassTag[T]
  }

  trait ContentLoader[T <: ContentAddressable] extends Loader[T]{
    override def asContentLoader : Option[ContentLoader[T with ContentAddressable]] = Some(this.asInstanceOf[ContentLoader[T with ContentAddressable]])
    def hash(trg:T):Hash = trg.hash
  }

  object InputLoader extends ContentLoader[InputSource] {
    override def load(src: GeneralSource): Option[InputSource]  = src match {
      case input: InputSource => Some(input)
      case _ => None
    }
    override def tag: ClassTag[InputSource] = implicitly[ClassTag[InputSource]]
  }

  object DirectoryLoader extends Loader[Directory] {
    override def load(src: GeneralSource): Option[Directory] = src match {
      case directory: Directory => Some(directory)
      case _ => None
    }
    override def tag: ClassTag[Directory] = implicitly[ClassTag[Directory]]
  }

  val protocol:Regex = """^(.*)://(.*)$""".r
  val pathSeparator:Regex = "[\\|/]".r
  def getProtocolHeader(protocolName:String):String = protocolName+"://"

  type PluginType = AddressResolver
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]

  override def category: PluginCategory[PluginType] = AddressResolverPluginCategory

  def provideDefault():Option[Directory] = {
    select(Selectors.Default).flatMap(r => r.provideDefault())
  }

  def resolveDirectory(parent:Directory, path:Address, create:Boolean = false):Option[Directory] = {
    select(Selectors.Lookup(parent, path, Selectors.LocationLookupMode) ).flatMap(r => r.resolveDirectory(parent, path, create))
  }

  def deleteDirectory(dir:Directory):Unit = {
    select(Selectors.Delete(dir)).foreach(p => p.deleteDirectory(dir))
  }

  def resolve[T <: Addressable](parent: Directory, path: Address, loader: Loader[T]):Option[T] = {
    select(Selectors.Lookup(parent, path, Selectors.SourceLookupMode) ).flatMap(r => r.resolve(parent, path, loader))
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
