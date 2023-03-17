package samaya.plugin.impl.pkg.json

import samaya.plugin.impl.pkg.json.JsonModel.Source
import samaya.plugin.service.AddressResolver
import samaya.structure.{Component, Interface}
import samaya.structure
import samaya.types.{Address, Identifier, Workspace}

object Serializer {

  def toPackageRepr(pkg: structure.LinkablePackage, workspace: Workspace): JsonModel.Package = {
    val basePath = AddressResolver.serializeDirectory(None, pkg.location)
    val dependencies = pkg.dependencies.flatMap(d => AddressResolver.serializeAddress(Some(pkg.location),d))
    JsonModel.Package(
      name = pkg.name,
      hash = pkg.hash.toString,
      components = pkg.components.map(toCompRepr),
      path = basePath,
      locations = toLocationsRepl(workspace),
      dependencies = dependencies
    )
  }

  def toCompRepr(cmp: Interface[Component]): JsonModel.Component = {
    JsonModel.Component(
      source = cmp.meta.sourceCode.map(_.identifier).map{
        case Identifier.General(name) => Source(name)
        case Identifier.Specific(name, extension) => Source(name, extension)
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
    val interface = AddressResolver.serializeDirectory(Some(workspace.location), workspace.interfaceLocation)
    val code = AddressResolver.serializeDirectory(Some(workspace.location), workspace.codeLocation)
    val source = AddressResolver.serializeDirectory(Some(workspace.location), workspace.sourceLocation)
    JsonModel.Locations(
      interface = interface.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      code = code.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      source = source.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL"))
    )
  }

}
