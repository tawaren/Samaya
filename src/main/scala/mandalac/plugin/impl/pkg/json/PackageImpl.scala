package mandalac.plugin.impl.pkg.json

import mandalac.structure.Module
import mandalac.structure.types.Hash
import mandalac.types
import mandalac.types.{Identifier, Location}

class PackageImpl(parsed:JsonModel.Package, override val path:Seq[Identifier], override val modules:Seq[Module], override val dependencies:Seq[types.Package]) extends types.Package {
  override val name: String = parsed.name
  override val hash: Hash = Hash.fromString(parsed.hash)

  //todo: Implement where to get from? Can we pass in?
  override def location: Location = ???
}