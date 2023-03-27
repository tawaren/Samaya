package samaya.plugin.service

import samaya.plugin.service.category.PackageEncodingPluginCategory
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.LinkablePackage
import samaya.toolbox.helpers.{ConcurrentComputationCache, ConcurrentStrongComputationCache}
import samaya.types.{GeneralSource, InputSource, Workspace}

import scala.reflect.ClassTag

trait PackageEncoder extends Plugin{

  override type Selector = Selectors.PackageSelector

  //register a Package (includes parsing and validating & resolving dependencies)
  // parent is the package path
  // location & name defines where to look for it
  // for top level packages empty list can be used as parent
  def deserializePackage(source:GeneralSource): Option[LinkablePackage]
  //writes output
  def serializePackage(pkg: LinkablePackage, workspace: Option[Workspace] = None): Option[InputSource]
}

object PackageEncoder extends PackageEncoder with PluginProxy{

  val packageExtensionPrefix = "pkg"

  val computationCache = new ConcurrentStrongComputationCache[GeneralSource,Option[LinkablePackage]]()

  //Todo: Move to Package or move the others to encoders
  object Loader extends AddressResolver.ContentLoader[LinkablePackage] {
    override def load(src: GeneralSource): Option[LinkablePackage] = deserializePackage(src)
    override def tag: ClassTag[LinkablePackage] = implicitly[ClassTag[LinkablePackage]]
  }

  type PluginType = PackageEncoder
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = PackageEncodingPluginCategory

  override def deserializePackage(source:GeneralSource): Option[LinkablePackage] = {
    computationCache.getOrElseUpdate(source){
      select(Selectors.PackageDecoderSelector(source)).flatMap(r => r.deserializePackage(source))
    }
  }

  override def serializePackage(pkg: LinkablePackage, workspace: Option[Workspace] = None): Option[InputSource]  = {
    select(Selectors.PackageEncoderSelector).flatMap(r => r.serializePackage(pkg, workspace))
  }

  def isPackage(source:GeneralSource): Boolean = {
    matches(Selectors.PackageDecoderSelector(source))
  }

}
