package mandalac.plugin.impl.pkg.json

import mandalac.plugin.service.LocationResolver
import mandalac.structure.Module
import mandalac.types
import mandalac.types.{Location, Workspace}

object Serializer {

  def toPackageRepr(parent:Location, pkg: types.Package, workspace: Workspace): JsonModel.Package = {
    val dependencies = pkg.dependencies.map(d => LocationResolver.serializeLocation(pkg.location, d.location))
    JsonModel.Package(
      name = pkg.name,
      hash = pkg.hash.toString,
      modules = pkg.modules.map(toModuleRepr),
      locations = toLocationsRepl(parent,workspace),
      dependencies = dependencies.map(l => l.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")))
    )
  }

  def toModuleRepr(module: Module): JsonModel.Module = {
    JsonModel.Module(
      name = module.name,
      hash = toHashesRepr(module),
      info = toInfoRepr(module)
    )
  }

  def toHashesRepr(module: Module): JsonModel.Hashes = {
    JsonModel.Hashes(
      interface = module.meta.interfaceHash.toString,
      code = module.hash.toString,
      source = module.meta.sourceHash.toString
    )
  }

  def toInfoRepr(module: Module): JsonModel.Info = {
    JsonModel.Info(
      language = module.language,
      version = module.version,
      classifier = module.classifier
    )
  }

  def toDependencyRepr(parent:Location, pkg: types.Package): String = {
    LocationResolver.serializeLocation(parent, pkg.location).getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL"))
  }

  def toLocationsRepl(parent:Location, workspace: Workspace): JsonModel.Locations = {
    val interface = LocationResolver.serializeLocation(parent,  workspace.interfaceLocation)
    val code = LocationResolver.serializeLocation(parent,  workspace.codeLocation)
    val source = LocationResolver.serializeLocation(parent,  workspace.sourceLocation)
    JsonModel.Locations(
      interface = interface.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      code = code.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      source = source.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL"))
    )
  }

}