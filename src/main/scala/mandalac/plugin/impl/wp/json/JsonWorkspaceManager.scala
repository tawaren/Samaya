package mandalac.plugin.impl.wp.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import mandalac.compilation.ErrorHandler
import mandalac.compilation.ErrorHandler.SimpleMessage
import mandalac.plugin.service.{LocationResolver, PackageManager, Selectors, WorkspaceManager}
import mandalac.types
import mandalac.types.{Identifier, InputSource, Location, Package, Path}

//A Workspace Manager for a json description of a package
class JsonWorkspaceManager extends WorkspaceManager {

  override def deserializeWorkspace(file: InputSource): Option[types.Workspace] = {
    deserializeWorkspace(List.empty,file)
  }
  //todo: we need path
  def deserializeWorkspace(path:List[Identifier], file: InputSource): Option[types.Workspace] = {
    //parse the package with Package as description
    val parsed = readFromStream[Workspace](file.content)
    val name: String = parsed.name
    val subPath = path :+ Identifier(name)
    val workspaceLocation: Location = file.location

    def toInputSources(paths:Option[Seq[String]]): Option[Set[InputSource]] = {
      paths.map(p => p
        .toSet[String]
        .flatMap(LocationResolver.parsePath)
        .flatMap(l =>  LocationResolver.resolveSource(workspaceLocation,l))
      )
    }

    //todo: if something gets lost we ned an error
    val includes: Option[Set[types.Workspace]] = toInputSources(parsed.includes).map(inputs => inputs.flatMap(input => deserializeWorkspace(subPath,input)))
    //todo: if something gets lost we ned an error
    val dependencies: Option[Set[Package]]  = toInputSources(parsed.dependencies).map(inputs => inputs.flatMap(input => PackageManager.registerPackage(path, input)))
    //todo: if something gets lost we ned an error
    val modules: Option[Set[Path]] = parsed.modules.map(s => s.toSet.flatMap(LocationResolver.parsePath))

    val sourceIdent = LocationResolver.parsePath(parsed.locations.source)match {
      case Some(id) => id
      case None =>
        ErrorHandler.feedback(SimpleMessage("Could not parse source path", ErrorHandler.Error))
        return None

    }

    val codeIdent = LocationResolver.parsePath(parsed.locations.code)match {
      case Some(id) => id
      case None =>
        ErrorHandler.feedback(SimpleMessage("Could not parse code path", ErrorHandler.Error))
        return None
    }

    val interfaceIdent = LocationResolver.parsePath(parsed.locations.interface)match {
      case Some(id) => id
      case None =>
        ErrorHandler.feedback(SimpleMessage("Could not parse interface path", ErrorHandler.Error))
        return None
    }

    val sourceLocation: Location = LocationResolver.resolveLocation(workspaceLocation,sourceIdent) match {
      case Some(loc) => loc
      case None =>
        ErrorHandler.feedback(SimpleMessage("Could not load source location", ErrorHandler.Error))
        return None
    }

    val codeLocation: Location = LocationResolver.resolveLocation(workspaceLocation,codeIdent)  match {
      case Some(loc) => loc
      case None =>
        ErrorHandler.feedback(SimpleMessage("Could not load code location", ErrorHandler.Error))
        return None
    }

    val interfaceLocation: Location = LocationResolver.resolveLocation(workspaceLocation,interfaceIdent)  match {
      case Some(loc) => loc
      case None =>
        ErrorHandler.feedback(SimpleMessage("Could not load interface location", ErrorHandler.Error))
        return None
    }
    Some(new WorkspaceImpl(name, workspaceLocation, includes, dependencies, modules, sourceLocation, codeLocation, interfaceLocation))
  }

  override def matches(s: Selectors.WorkspaceSelector): Boolean = {
    s match {
      case Selectors.WorkspaceDeserializationSelector(source) => source.identifier.extension.contains("json")
    }
  }
}
