package samaya.plugin.impl.wp.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager._
import samaya.plugin.service.WorkspaceEncoder.WorkSpaceExtension
import samaya.plugin.service.{AddressResolver, PackageEncoder, Selectors, WorkspaceEncoder}
import samaya.structure.{ContentAddressable, LinkablePackage, Package}
import samaya.types
import samaya.types.{Address, Identifier, InputSource, Directory}

//A Workspace Manager for a json description of a package
class JsonWorkspaceEncoder extends WorkspaceEncoder {

  val Json = "json"
  override def matches(s: Selectors.WorkspaceSelector): Boolean = {
    s match {
      case Selectors.WorkspaceDeserializationSelector(WorkSpaceExtension(Json)) => true
      case _ => false
    }
  }

  var cycleBreaker:Set[(Directory,String)] = Set.empty

  def deserializeWorkspace(file: InputSource): Option[types.Workspace] = {
    //parse the package with Package as description
    val parsed = readFromStream[Workspace](file.content)
    val name: String = parsed.name
    val workspaceLocation: Directory = file.location



    def toContent[T <: ContentAddressable](paths:Option[Seq[String]], loader:AddressResolver.Loader[T], extensionFilter:Option[Set[String]]): Option[Set[T]] = {
      paths.map(p => p
        .toSet[String]
        .flatMap(AddressResolver.parsePath)
        .flatMap(l =>  AddressResolver.resolve(workspaceLocation,l, loader,extensionFilter))
      )
    }

    val oldBreaker = cycleBreaker
    val (includes, dependencies) = if(!oldBreaker.contains((workspaceLocation, name))) {
      cycleBreaker = oldBreaker + ((workspaceLocation, name))
      val includes: Option[Set[types.Workspace]] = toContent(parsed.includes, AddressResolver.InputLoader, Some(Set(WorkspaceEncoder.workspaceExtensionPrefix))).map(
        inputs => inputs.flatMap(
          input => WorkspaceEncoder.deserializeWorkspace(input)
        )
      )
      if(includes.getOrElse(Set.empty).size != parsed.includes.getOrElse(Set.empty).size) {
        feedback(PlainMessage(s"Could not deserialize all includes", Warning, Builder()))
      }

      val dependencies: Option[Set[LinkablePackage]] = toContent(parsed.dependencies, PackageEncoder.Loader, Some(Set(PackageEncoder.packageExtensionPrefix)))
      if(dependencies.getOrElse(Set.empty).size != parsed.dependencies.getOrElse(Set.empty).size) {
        feedback(PlainMessage(s"Could not deserialize all dependencies", Warning, Builder()))
      }
      cycleBreaker = oldBreaker
      (includes, dependencies)
    } else {
      feedback(PlainMessage(s"Cyclic dependencies are not allowed. $name depends on itself", Error, Builder()))
      (None, None)
    }


    //todo: if something gets lost we ned an error
    val components: Option[Set[Address]] = parsed.sources.map(
      s => s.toSet.flatMap(AddressResolver.parsePath)
    )

    val codeIdent = AddressResolver.parsePath(parsed.locations.code)match {
      case Some(id) => id
      case None =>
        ErrorManager.feedback(PlainMessage("Could not parse code path", ErrorManager.Error, Builder()))
        return None
    }

    val interfaceIdent = AddressResolver.parsePath(parsed.locations.interface)match {
      case Some(id) => id
      case None =>
        ErrorManager.feedback(PlainMessage("Could not parse interface path", ErrorManager.Error, Builder()))
        return None
    }

    val sourceLocation: Directory = parsed.locations.source match {
        case None => workspaceLocation:Directory
        case Some(path) =>
          val sourceIdent = AddressResolver.parsePath(path) match {
            case Some(id) => id
            case None =>
              ErrorManager.feedback(PlainMessage("Could not parse source path", ErrorManager.Error, Builder()))
              return None

          }

          AddressResolver.resolveDirectory(workspaceLocation,sourceIdent) match {
            case Some(loc) => loc
            case None =>
              ErrorManager.feedback(PlainMessage("Could not load source location", ErrorManager.Error, Builder()))
              return None
          }
    }

    val codeLocation: Directory = AddressResolver.resolveDirectory(workspaceLocation,codeIdent, create = true)  match {
      case Some(loc) => loc
      case None =>
        ErrorManager.feedback(PlainMessage("Could not load code location", ErrorManager.Error, Builder()))
        return None
    }

    val interfaceLocation: Directory = AddressResolver.resolveDirectory(workspaceLocation,interfaceIdent, create = true)  match {
      case Some(loc) => loc
      case None =>
        ErrorManager.feedback(PlainMessage("Could not load interface location", ErrorManager.Error, Builder()))
        return None
    }
    Some(new WorkspaceImpl(name, workspaceLocation, includes, dependencies, components, sourceLocation, codeLocation, interfaceLocation))
  }
}
