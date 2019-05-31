package mandalac.plugin.service

import mandalac.plugin.service.category.PackageEncodingPluginCategory
import mandalac.plugin.{Plugin, PluginProxy}
import mandalac.types
import mandalac.types.{Identifier, InputSource, Location, Workspace}

trait PackageManager extends Plugin{

  override type Selector = Selectors.PackageSelector


  //register a Package (includes parsing and validating & resolving dependencies)
  // parent is the package path
  // location & name defines where to look for it
  // for top level packages empty list can be used as parent
  //todo: make a simple deserialize and do the rest in a common part
  def registerPackage(parent:Seq[Identifier], input:InputSource): Option[types.Package]
  //writes output
  def serializePackage(parent:Location, pkg: types.Package, workspace: Workspace): Boolean
}

object PackageManager extends PackageManager with PluginProxy{

  type PluginType = PackageManager
  override def category: PluginCategory[PluginType] = PackageEncodingPluginCategory

  override def registerPackage(parent:Seq[Identifier], input:InputSource): Option[types.Package] = {
    select(Selectors.PackageDeserializationSelector(input)).flatMap(r => r.registerPackage(parent, input))
  }


  override def serializePackage(parent:Location, pkg: types.Package, workspace:Workspace): Boolean  = {
    //todo: get default from config
    serializePackage(parent, pkg, workspace,"json")
  }

  def serializePackage(parent:Location, pkg: types.Package, workspace: Workspace, format:String): Boolean  = {
    select(Selectors.PackageSerializationSelector(format)).exists(r => r.serializePackage(parent, pkg, workspace))
  }

}