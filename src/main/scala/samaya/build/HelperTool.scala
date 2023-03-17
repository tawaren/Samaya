package samaya.build

import samaya.plugin.service.AddressResolver
import samaya.types.{Directory, Workspace}
import samaya.validation.WorkspaceValidator

object HelperTool {

  def createWorkspace(target:String):Workspace = {
    //todo: have a multi location selector
    val parent:Directory = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = AddressResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    AddressResolver.resolve(parent, ident, Workspace.Loader) match {
      case None => throw new Exception("Workspace not found");
      case Some(value) =>
        WorkspaceValidator.validateWorkspace(value)
        value
    }
  }
}
