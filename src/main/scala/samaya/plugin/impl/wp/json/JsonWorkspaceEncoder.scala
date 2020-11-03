package samaya.plugin.impl.wp.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager._
import samaya.plugin.service.WorkspaceEncoder.WorkSpaceExtension
import samaya.plugin.service.{LocationResolver, PackageEncoder, Selectors, WorkspaceEncoder}
import samaya.structure.{LinkablePackage, Package}
import samaya.types
import samaya.types.{Identifier, InputSource, Location, Path}

//A Workspace Manager for a json description of a package
class JsonWorkspaceEncoder extends WorkspaceEncoder {

  val Json = "json"
  override def matches(s: Selectors.WorkspaceSelector): Boolean = {
    s match {
      case Selectors.WorkspaceDeserializationSelector(WorkSpaceExtension(Json)) => true
      case _ => false
    }
  }

  var cycleBreaker:Set[(Location,String)] = Set.empty

  def deserializeWorkspace(file: InputSource): Option[types.Workspace] = {
    //parse the package with Package as description
    val parsed = readFromStream[Workspace](file.content)
    val name: String = parsed.name
    val workspaceLocation: Location = file.location

    def toInputSources(paths:Option[Seq[String]], extensionFilter:Option[Set[String]]): Option[Set[InputSource]] = {
      paths.map(p => p
        .toSet[String]
        .flatMap(LocationResolver.parsePath)
        .flatMap(l =>  LocationResolver.resolveSource(workspaceLocation,l, extensionFilter))
      )
    }

    val oldBreaker = cycleBreaker
    val (includes, dependencies) = if(!oldBreaker.contains((workspaceLocation, name))) {
      cycleBreaker = oldBreaker + ((workspaceLocation, name))
      val includes: Option[Set[types.Workspace]] = toInputSources(parsed.includes, Some(Set(WorkspaceEncoder.workspaceExtensionPrefix))).map(
        inputs => inputs.flatMap(
          input => WorkspaceEncoder.deserializeWorkspace(input)
        )
      )
      if(includes.getOrElse(Set.empty).size != parsed.includes.getOrElse(Set.empty).size) {
        feedback(PlainMessage(s"Could not deserialize all includes", Warning))
      }

      val dependencies: Option[Set[LinkablePackage]] = toInputSources(parsed.dependencies, Some(Set(PackageEncoder.packageExtensionPrefix))).map(
        inputs => inputs.flatMap(
          input => PackageEncoder.deserializePackage(input)
        )
      )
      if(dependencies.getOrElse(Set.empty).size != parsed.dependencies.getOrElse(Set.empty).size) {
        feedback(PlainMessage(s"Could not deserialize all dependencies", Warning))
      }
      cycleBreaker = oldBreaker
      (includes, dependencies)
    } else {
      feedback(PlainMessage(s"Cyclic dependencies are not allowed. $name depends on itself", Error))
      (None, None)
    }


    //todo: if something gets lost we ned an error
    val components: Option[Set[Path]] = parsed.components.map(
      s => s.toSet.flatMap(LocationResolver.parsePath)
    )

    val sourceIdent = LocationResolver.parsePath(parsed.locations.source)match {
      case Some(id) => id
      case None =>
        ErrorManager.feedback(PlainMessage("Could not parse source path", ErrorManager.Error))
        return None

    }

    val codeIdent = LocationResolver.parsePath(parsed.locations.code)match {
      case Some(id) => id
      case None =>
        ErrorManager.feedback(PlainMessage("Could not parse code path", ErrorManager.Error))
        return None
    }

    val interfaceIdent = LocationResolver.parsePath(parsed.locations.interface)match {
      case Some(id) => id
      case None =>
        ErrorManager.feedback(PlainMessage("Could not parse interface path", ErrorManager.Error))
        return None
    }

    val sourceLocation: Location = LocationResolver.resolveLocation(workspaceLocation,sourceIdent) match {
      case Some(loc) => loc
      case None =>
        ErrorManager.feedback(PlainMessage("Could not load source location", ErrorManager.Error))
        return None
    }

    val codeLocation: Location = LocationResolver.resolveLocation(workspaceLocation,codeIdent, create = true)  match {
      case Some(loc) => loc
      case None =>
        ErrorManager.feedback(PlainMessage("Could not load code location", ErrorManager.Error))
        return None
    }

    val interfaceLocation: Location = LocationResolver.resolveLocation(workspaceLocation,interfaceIdent, create = true)  match {
      case Some(loc) => loc
      case None =>
        ErrorManager.feedback(PlainMessage("Could not load interface location", ErrorManager.Error))
        return None
    }
    Some(new WorkspaceImpl(name, workspaceLocation, includes, dependencies, components, sourceLocation, codeLocation, interfaceLocation))
  }
}
