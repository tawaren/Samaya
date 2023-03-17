package samaya.plugin.service

import samaya.plugin.service.AddressResolver.SerializerMode
import samaya.structure.Component
import samaya.structure.types.Hash
import samaya.types.{Address, ContentAddressable, Directory, GeneralSource, Identifier, InputSource}

//todo: Shall we make seperate objects and subfolders
//   Shall we merge certain selectors that hav same layout??
//   note: it can not be in the corresponding implementere because of cyclic dependencies and other stuff
//         we tried, we even get it compiled but not running
object Selectors {
  sealed trait InterfaceSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class InterfaceDeserializationSelector(language:String, version:String, classifier:Set[String], source:InputSource) extends InterfaceSelector
  //A Interface serialization Task description intended for selecting the appropriate plugin
  case class InterfaceSerializationSelector(language:String, version:String, classifier:Set[String], format:String) extends InterfaceSelector

  sealed trait WorkspaceSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class WorkspaceDecoderSelector(source:GeneralSource) extends WorkspaceSelector

  sealed trait DependenciesImportSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class DependenciesDecoderSelector(source : GeneralSource) extends DependenciesImportSelector

  sealed trait RepositoriesImportSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class RepositoriesDecoderSelector(source: GeneralSource) extends RepositoriesImportSelector


  sealed trait PackageSelector
  //A Package loading Task description intended for selecting the appropriate plugin
  case class PackageDeserializationSelector(source:InputSource) extends PackageSelector
  //A Package serializer Task description intended for selecting the appropriate plugin
  case class PackageSerializationSelector(format:String) extends PackageSelector

  case class DebugAssemblerSelector(target:Component)

  sealed trait DeployerSelector
  case object ModuleDeployerSelector extends DeployerSelector
  case object TransactionDeployerSelector extends DeployerSelector

  sealed trait AddressSelector
  //A Location resolution Task description intended for selecting the appropriate plugin
  sealed trait LookupMode
  case object LocationLookupMode extends LookupMode
  case object SourceLookupMode extends LookupMode
  case object SinkLookupMode extends LookupMode

  case class Lookup(parent:Directory, path:Address, mode:LookupMode) extends AddressSelector
  //todo: add mode
  case class Delete(dir:Directory) extends AddressSelector
  case class List(parent:Directory) extends AddressSelector
  case class SerializeAddress(parent:Option[Directory], target: ContentAddressable, mode:SerializerMode) extends AddressSelector
  case class SerializeDirectory(parent:Option[Directory], target: Directory) extends AddressSelector
  case class Parse(name:String) extends AddressSelector

  case object Default extends AddressSelector

  sealed trait ContentSelector
  case object UpdateContentIndex extends ContentSelector
  case class StoreContentIndex(directory:Directory) extends ContentSelector

  sealed trait RepositoryLoaderSelector
  case class LoadRepository(source:GeneralSource) extends RepositoryLoaderSelector

  trait CompilerSelector
  case class CompilerSelectorByMeta(language:String, version:String, classifier:Set[String]) extends CompilerSelector
  case class CompilerSelectorBySource(source: InputSource)  extends CompilerSelector
  case class CompilerSelectorByIdentifier(source: Identifier)  extends CompilerSelector

  case class ValidatorSelector(target:Component)

}
