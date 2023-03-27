package samaya.plugin.impl.pkg.json

import samaya.plugin.service.AddressResolver
import samaya.plugin.service.AddressResolver.{DynamicLocation, Hybrid, RelativeLocation}
import samaya.structure.{Component, Interface}
import samaya.structure
import samaya.types.{Address, Directory, Identifier, Workspace}

object Serializer {

  def toPackageRepr(pkg: structure.LinkablePackage, workspace: Option[Workspace]): JsonModel.Package = {
    val basePath = AddressResolver.serializeDirectoryAddress(pkg.location)
    val dependencies = pkg.dependencies.flatMap(d => AddressResolver.serializeContentAddress(d,Hybrid(RelativeLocation(pkg.location))))
    JsonModel.Package(
      name = pkg.name,
      hash = pkg.hash.toString,
      components = pkg.components.map(toCompRepr),
      path = basePath,
      locations = workspace.map(toLocationsRepl(pkg.location, _)),
      dependencies = dependencies,
      includes = pkg.includes
    )
  }

  def toCompRepr(cmp: Interface[Component]): JsonModel.Component = {
    JsonModel.Component(
      source = cmp.meta.sourceCode.map(_.identifier.fullName),
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

  def toLocationsRepl(pkgLocation:Directory, workspace: Workspace): JsonModel.Locations = {
    val interface = AddressResolver.serializeDirectoryAddress(workspace.interfaceLocation, DynamicLocation(pkgLocation))
    val code = AddressResolver.serializeDirectoryAddress(workspace.codeLocation, DynamicLocation(pkgLocation))
    val source = AddressResolver.serializeDirectoryAddress(workspace.sourceLocation, DynamicLocation(pkgLocation))
    JsonModel.Locations(
      interface = interface.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      code = code.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL")),
      source = source.getOrElse(throw new Exception("MAKE CUSTOM ONE OR BETTER SER MODEL"))
    )
  }

}
