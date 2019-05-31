package mandalac.plugin.service

import mandalac.types.{Identifier, InputSource, Location, Path}

//todo: Shall we make seperate objects and subfolders
//   note: it can not be in the corresponding implementere because of cyclic dependencies and other stuff
//         we tried, we even get it compiled but not running
object Selectors {
  sealed trait InterfaceSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class InterfaceDeserializationSelector(source:InputSource) extends InterfaceSelector
  //A Interface serialization Task description intended for selecting the appropriate plugin
  case class InterfaceSerializationSelector(format:String) extends InterfaceSelector

  sealed trait WorkspaceSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class WorkspaceDeserializationSelector(source:InputSource) extends WorkspaceSelector

  sealed trait PackageSelector
  //A Package loading Task description intended for selecting the appropriate plugin
  case class PackageDeserializationSelector(source:InputSource) extends PackageSelector
  //A Package serializer Task description intended for selecting the appropriate plugin
  case class PackageSerializationSelector(format:String) extends PackageSelector

  sealed trait LocationSelector
  //A Location resolution Task description intended for selecting the appropriate plugin
  case class Lookup(parent:Location, path:Path) extends LocationSelector
  case class List(parent:Location) extends LocationSelector
  case class Serialize(parent:Location, target: Location) extends LocationSelector
  case class Parse(name:String) extends LocationSelector

  case object Default extends LocationSelector

  trait CompilerSelector
  case class CompilerSelectorByMeta(language:String, version:String, classifier:String) extends CompilerSelector
  case class CompilerSelectorBySource(source: InputSource)  extends CompilerSelector


}
