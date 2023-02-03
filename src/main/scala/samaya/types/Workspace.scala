package samaya.types

import samaya.structure.LinkablePackage

//todo: have a way to create the whole thing from just a directory by using conventions
//       Maybe a WorkspaceConventionPlugin???
// todo: if we do the convention stuff we can ask the plugin for defaults
trait Workspace {
  //the folder in which we operate
  def workspaceLocation:Directory
  //dependencies
  //local dependency that are recompiled
  def includes:Option[Set[Workspace]]
  //external dependency that is just linked
  def dependencies:Option[Set[LinkablePackage]]
  //name of the resulting package
  //defaults to workspace name if not explicitly provided
  def name:String
  //sources contained in the package
  def sources: Option[Set[Address]]
  // the place to look for sources
  def sourceLocation:Directory
  //targets
  def codeLocation:Directory
  def interfaceLocation:Directory
}
