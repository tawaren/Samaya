package samaya.plugin.service

import samaya.plugin.service.AddressResolver.PluginType
import samaya.plugin.service.category.PackageEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.LinkablePackage
import samaya.structure.types.Hash
import samaya.types.{InputSource, Workspace}

import scala.reflect.ClassTag

trait PackageEncoder extends Plugin{

  override type Selector = Selectors.PackageSelector

  //register a Package (includes parsing and validating & resolving dependencies)
  // parent is the package path
  // location & name defines where to look for it
  // for top level packages empty list can be used as parent
  //todo: make a simple deserialize and do the rest in a common part
  def deserializePackage(input:InputSource): Option[LinkablePackage]
  //writes output
  def serializePackage(pkg: LinkablePackage, workspace: Workspace): Option[InputSource]
}

object PackageEncoder extends PackageEncoder with PluginProxy{

  object Loader extends AddressResolver.Loader[LinkablePackage] {
    override def load(src: InputSource): Option[LinkablePackage] = deserializePackage(src)
  }

  val packageExtensionPrefix = "pkg"

  object PackageExtension {
    def apply(format: String): String = packageExtensionPrefix + "." + format
    def unapply(ext: String): Option[String] = if(!ext.startsWith(packageExtensionPrefix)) {
      None
    } else {
      Some(ext.drop(packageExtensionPrefix.length + 1))
    }
    def unapply(source: InputSource): Option[String] = source.identifier.extension.flatMap(unapply)
  }

  type PluginType = PackageEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = PackageEncodingPluginCategory

  override def deserializePackage(input:InputSource): Option[LinkablePackage] = {
    select(Selectors.PackageDeserializationSelector(input)).flatMap(r => r.deserializePackage(input))
  }

  override def serializePackage(pkg: LinkablePackage, workspace:Workspace): Option[InputSource]  = {
    //todo: get default from config
    serializePackage(pkg, workspace,PackageExtension("json"))
  }

  def serializePackage(pkg: LinkablePackage, workspace: Workspace, format:String): Option[InputSource]  = {
    select(Selectors.PackageSerializationSelector(format)).flatMap(r => r.serializePackage(pkg, workspace))
  }

}
