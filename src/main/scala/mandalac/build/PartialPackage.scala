package mandalac.build

import mandalac.structure.Module
import mandalac.structure.types.Hash
import mandalac.types
import mandalac.types.{Identifier, Location}

case class PartialPackage(
                           override val path: Seq[Identifier],
                           override val location: Location,
                           override val name: String,
                           override val dependencies: Seq[types.Package],
                           override val modules: Seq[Module] = Seq.empty,
                           pkgHash:Option[Hash] = None
) extends types.Package{

  override def hash: Hash = pkgHash.getOrElse(throw new Exception("NEEDS CUSTOM: NOT AVAIABLE YET"))
  def withModule(module:Module):PartialPackage = copy(modules = modules :+ module)
  def withHash(hash:Hash):types.Package = copy(pkgHash = Some(hash))

}
