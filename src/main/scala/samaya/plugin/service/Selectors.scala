package samaya.plugin.service

import samaya.plugin.service.AddressResolver.SerializerMode
import samaya.plugin.service.ReferenceResolver.ReferenceType
import samaya.structure.Component
import samaya.structure.types.Hash
import samaya.types.{Address, ContentAddressable, Directory, GeneralSink, GeneralSource, Identifier, InputSource}

import scala.reflect.ClassTag

//todo: Shall we make seperate objects and subfolders
//   Shall we merge certain selectors that hav same layout??
//   note: it can not be in the corresponding implementere because of cyclic dependencies and other stuff
//         we tried, we even get it compiled but not running
object Selectors {
  sealed trait InterfaceSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class InterfaceDecoderSelector(language:String, version:String, classifier:Set[String], source:InputSource) extends InterfaceSelector
  //A Interface serialization Task description intended for selecting the appropriate plugin
  case class InterfaceEncoderSelector(language:String, version:String, classifier:Set[String], format:String) extends InterfaceSelector
  //Selects the default format
  case object InterfaceFormatSelector extends InterfaceSelector


  sealed trait WorkspaceSelector
  //A Interface loading Task description intended for selecting the appropriate plugin
  case class WorkspaceDecoderSelector(source:GeneralSource) extends WorkspaceSelector

  sealed trait ReferenceResolverSelector
  case class ResolveAllReferencesSelector(source:GeneralSource, filter:Option[Set[ReferenceType]]) extends ReferenceResolverSelector
  case class ResolveSingleReferencesSelector(source:GeneralSource, typ:ReferenceType) extends ReferenceResolverSelector

  sealed trait PackageSelector
  //A Package loading Task description intended for selecting the appropriate plugin
  case class PackageDecoderSelector(source:GeneralSource) extends PackageSelector
  //A Package serializer Task description intended for selecting the appropriate plugin
  case object PackageEncoderSelector extends PackageSelector

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

  case class Lookup(path:Address, mode:LookupMode) extends AddressSelector
  //todo: add mode
  case class Delete(dir:Directory) extends AddressSelector
  case class List(parent:Directory) extends AddressSelector
  case class SerializeAddress(target: ContentAddressable, mode:SerializerMode) extends AddressSelector
  case class SerializeDirectory(target: Directory, mode:SerializerMode) extends AddressSelector
  case class Parse(name:String) extends AddressSelector

  case object Default extends AddressSelector

  sealed trait ContentIndexSelector
  case object UpdateContentIndex extends ContentIndexSelector
  case class StoreContentIndex(directory:Directory) extends ContentIndexSelector

  sealed trait RepositoryEncoderSelector
  case class LoadRepository(source:GeneralSource) extends RepositoryEncoderSelector
  case class CreateRepository(sink:GeneralSink) extends RepositoryEncoderSelector


  trait CompilerSelector
  case class CompilerSelectorByMeta(language:String, version:String, classifier:Set[String]) extends CompilerSelector
  case class CompilerSelectorBySource(source: InputSource)  extends CompilerSelector
  case class CompilerSelectorByIdentifier(source: Identifier)  extends CompilerSelector

  case class ValidatorSelector(target:Component)

  sealed trait JobExecutorSelector
  case object IndependentJobSelector extends JobExecutorSelector
  case object DependantJobSelector extends JobExecutorSelector

  sealed trait TaskExecutorSelector
  case class SelectByName(name:String) extends TaskExecutorSelector
  case class SelectApplyTask[S,T](name:String, src:ClassTag[S], res:ClassTag[T]) extends TaskExecutorSelector

}
