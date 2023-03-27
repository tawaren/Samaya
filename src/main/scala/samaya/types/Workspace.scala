package samaya.types

import samaya.plugin.service.{AddressResolver, WorkspaceEncoder}
import samaya.structure.LinkablePackage

import scala.reflect.ClassTag

//todo: have a way to create the whole thing from just a directory by using conventions
//       Maybe a WorkspaceConventionPlugin???
// todo: if we do the convention stuff we can ask the plugin for defaults
trait Workspace extends Addressable {
  //the folder in which we operate
  def location:Directory
  //where to generate package & index files
  def packageTarget:Directory
  //dependencies
  //local dependency that are recompiled
  def includes:Set[Workspace]
  //repositories to use for dependency lookups in this & subprojects
  def repositories:Set[Repository]
  //external dependency that is just linked
  def dependencies:Set[LinkablePackage]
  //name of the resulting package
  //defaults to workspace name if not explicitly provided
  def name:String
  override def identifier: Identifier = Identifier(name)
  //sources contained in the package
  def sources: Set[Address]
  // the place to look for sources
  def sourceLocation:Directory
  //targets
  def codeLocation:Directory
  def interfaceLocation:Directory
}

object Workspace {
  object Loader extends AddressResolver.Loader[Workspace]{
    override def load(src: GeneralSource): Option[Workspace] = WorkspaceEncoder.decodeWorkspace(src)
    override def tag: ClassTag[Workspace] = implicitly[ClassTag[Workspace]]
  }
}
