package samaya.plugin.impl.pkg.json

import samaya.plugin.impl.pkg.json.JsonModel.Source
import samaya.plugin.service.LocationResolver
import samaya.structure.{Component, Interface}
import samaya.structure
import samaya.types.{Identifier, Workspace}

object Serializer {

  def toPackageRepr(pkg: structure.LinkablePackage, workspace: Workspace): JsonModel.Package = {
    val basePath = LocationResolver.serialize(None, pkg.location, None)
    val dependencies = pkg.dependencies.map(d => LocationResolver.serialize(Some(pkg.location), d.location, Some(d.name)))
    JsonModel.Package(
      name = pkg.name,
      hash = pkg.hash.toString,
      components = pkg.components.map(toCompRepr),
      path = basePath,
      locations = toLocationsRepl(workspace),
      dependencies = dependencies.map(l => l.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")))
    )
  }

  def toCompRepr(cmp: Interface[Component]): JsonModel.Component = {
    JsonModel.Component(
      source = cmp.meta.sourceCode.map(_.identifier).map{
        case Identifier.General(name) => Source(name)
        case Identifier.Specific(name, extension) => Source(name, Some(extension))
      },
      name = cmp.name,
      hash = toHashesRepr(cmp),
      info = toInfoRepr(cmp)
    )
  }

  def toHashesRepr(cmp: Interface[Component]): JsonModel.Hashes = {
    JsonModel.Hashes(
      interface = cmp.meta.interfaceHash.toString,
      code = cmp.meta.codeHash.map(_.toString),
      source = cmp.meta.sourceHash.toString
    )
  }

  def toInfoRepr(minter: Interface[Component]): JsonModel.Info = {
    JsonModel.Info(
      language = minter.language,
      version = minter.version,
      classifier = minter.classifier
    )
  }

  def toLocationsRepl(workspace: Workspace): JsonModel.Locations = {
    val interface = LocationResolver.serialize(Some(workspace.workspaceLocation), workspace.interfaceLocation, None)
    val code = LocationResolver.serialize(Some(workspace.workspaceLocation), workspace.codeLocation, None)
    val source = LocationResolver.serialize(Some(workspace.workspaceLocation), workspace.sourceLocation, None)
    JsonModel.Locations(
      interface = interface.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      code = code.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      source = source.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL"))
    )
  }

}