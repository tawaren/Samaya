package mandalac.registries

import mandalac.types.{Identifier, Package}

import scala.collection.mutable

object PackageRegistry{

  private val byName = new mutable.HashMap[Seq[Identifier],Package]
  def packageByName(path: Seq[Identifier]):Option[Package] = byName.get(path)

  def recordPackage(path: Seq[Identifier],pkg:Package) = {
    byName.put(path,pkg)
  }


}
