package samaya.build

import ky.korins.blake3.Blake3

import samaya.structure.types.Hash
import samaya.structure.{Component, Interface, LinkablePackage, Package}
import samaya.types.Directory
case class PartialPackage(
                           override val name: String,
                           override val dependencies: Seq[LinkablePackage],
                           override val components: Seq[Interface[Component]] = Seq.empty,
) extends Package{
  def withComponent(comp:Interface[Component]):PartialPackage = copy(components = components :+ comp)

  def toLinkablePackage(placement:Directory): LinkablePackage = {

    val digest = Blake3.newHasher()
    components.map(e => e.meta.interfaceHash).sorted.distinct.foreach(h => digest.update(h.data))
    dependencies.sortBy(p => p.name).distinct.foreach(p => {
      digest.update(p.name.getBytes)
      digest.update(p.hash.data)
    })

    new LinkablePackage(
      false,
      placement,
      Hash.fromBytes(digest.done(Hash.byteLen)),
      name,
      components,
      dependencies,
    )
  }
}
