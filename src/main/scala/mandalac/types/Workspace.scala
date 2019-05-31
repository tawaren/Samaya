package mandalac.types

//todo have parsing plugin
//todo: have a way to create the whole thing from just a directory by using conventions
//       Maybe a WorkspaceConventionPlugin???
// todo: if we do the convention stuff we can ask the plugin for defaults
trait Workspace {
  //the folder in which we operate
  def workspaceLocation:Location
  //dependencies
  //local dependency that are recompiled
  def includes:Option[Set[Workspace]]
  //external dependency that is just linked
  def dependencies:Option[Set[Package]]
  //name of the resulting package
  //defaults to workspace name if not explicitly provided
  def name:String
  //modules contained in the package
  def modules:Option[Set[Path]]
  // the place to look for sources
  def sourceLocation:Location
  //targets
  def codeLocation:Location
  def interfaceLocation:Location
}
