package samaya.plugin.service


import samaya.plugin.service.AddressResolver.{AddressKind, DirectoryMode, Exists, SerializerMode}
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
  def resolveDirectory(path:Address, mode:DirectoryMode = Exists):Option[Directory]
  //delete a existing location from a parent location and the sub location name
  def deleteDirectory(dir:Directory):Unit
  //finds a source
  def resolve[T <: Addressable](path: Address, loader: AddressResolver.Loader[T]):Option[T]
  //finds a sink
  def resolveSink(parent:Directory, ident:Identifier.Specific):Option[OutputTarget]
  //Todo: a DeleteSink
  //list
  def list(parent:Directory, filter:Option[AddressKind]):Set[Identifier]
  //parse
  def parsePath(ident:String):Option[Address]
  //serialize - if the parent is present a relative path is generated if possible
  def serializeContentAddress(target:ContentAddressable, mode:SerializerMode):Option[String]
  def serializeDirectoryAddress(target: Directory, mode:SerializerMode): Option[String]
  //provide a default location
  def provideDefault():Option[Directory]
}

object AddressResolver extends AddressResolver with PluginProxy{

  sealed trait SerializerMode
  case object Content extends SerializerMode
  trait LocationMode extends  SerializerMode
  case object AbsoluteLocation extends LocationMode
  case class DynamicLocation(parent:Directory) extends LocationMode
  case class RelativeLocation(parent:Directory) extends LocationMode
  case class Hybrid(locPart:LocationMode) extends SerializerMode

  sealed trait AddressKind
  case object Directory extends AddressKind
  case object Element extends AddressKind

  sealed trait DirectoryMode
  case object Exists extends DirectoryMode
  case object Create extends DirectoryMode
  case object ReCreate extends DirectoryMode


  trait Loader[T <: Addressable] {
    type Result = T
    def asContentLoader : Option[ContentLoader[Result with ContentAddressable]] = None
    def load(src:GeneralSource):Option[Result]
    def tag:ClassTag[Result]
  }

  trait ContentLoader[T <: ContentAddressable] extends Loader[T]{
    override def asContentLoader : Option[ContentLoader[Result with ContentAddressable]] = Some(this.asInstanceOf[ContentLoader[Result with ContentAddressable]])
    def hash(trg:Result):Hash = trg.hash
  }

  //Todo: Move these into the corresponding companion objects
  object SourceLoader extends Loader[GeneralSource] {
    override def load(src: GeneralSource): Option[GeneralSource] = Some(src)
    override def tag: ClassTag[GeneralSource] = implicitly[ClassTag[GeneralSource]]
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

  case class ContentExtensionLoader[T <: ContentAddressable](loader:ContentLoader[T], extension:String) extends ContentLoader[T] {
    override def load(src: GeneralSource): Option[Result] = if(src.identifier.extension.exists(_.startsWith(extension))){
      loader.load(src)
    } else {
      None
    }
    override def tag: ClassTag[Result] = loader.tag
  }


  val protocol:Regex = """^(.*)://(.*)$""".r
  val pathSeparator:Regex = "[\\\\|/]".r
  def getProtocolHeader(protocolName:String):String = protocolName+"://"

  type PluginType = AddressResolver
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]

  override def category: PluginCategory[PluginType] = AddressResolverPluginCategory

  def provideDefault():Option[Directory] = {
    select(Selectors.Default).flatMap(r => r.provideDefault())
  }

  def resolveDirectory(path:Address, mode:DirectoryMode = Exists):Option[Directory] = {
    select(Selectors.Lookup(path, Selectors.LocationLookupMode) ).flatMap(r => r.resolveDirectory(path, mode))
  }

  def deleteDirectory(dir:Directory):Unit = {
    select(Selectors.Delete(dir)).foreach(p => p.deleteDirectory(dir))
  }

  def resolve[T <: Addressable](path: Address, loader: Loader[T]):Option[T] = {
    select(Selectors.Lookup(path, Selectors.SourceLookupMode)).flatMap(r => r.resolve(path, loader))
  }

  def resolveSink(parent:Directory, ident:Identifier.Specific):Option[OutputTarget] = {
    select(Selectors.Lookup(parent.resolveAddress(Address(ident)) , Selectors.SinkLookupMode) ).flatMap(r => r.resolveSink(parent, ident))
  }

  def list(parent:Directory, filter:Option[AddressKind] = None):Set[Identifier] = {
    select(Selectors.List(parent)).map(r => r.list(parent, filter)).getOrElse(Set.empty)
  }

  override def parsePath(ident: String): Option[Address] = {
    select(Selectors.Parse(ident)).flatMap(r => r.parsePath(ident))
  }

  override def serializeContentAddress(target: ContentAddressable, mode: SerializerMode = Content): Option[String] = {
    select(Selectors.SerializeAddress(target, mode)).flatMap(r => r.serializeContentAddress(target, mode))
  }

  override def serializeDirectoryAddress(target: Directory, mode: SerializerMode = AbsoluteLocation): Option[String] = {
    select(Selectors.SerializeDirectory(target, mode)).flatMap(r => r.serializeDirectoryAddress(target, mode))
  }

}
