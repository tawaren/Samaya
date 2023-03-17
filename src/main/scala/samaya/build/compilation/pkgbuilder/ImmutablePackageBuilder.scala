package samaya.build.compilation.pkgbuilder

import ky.korins.blake3.Blake3
import samaya.build.PartialPackage
import samaya.structure.{Component, Interface, LinkablePackage, Package}
import samaya.structure.types.Hash
import samaya.types.Directory

case class ImmutablePackageBuilder(
                           override val name: String,
                           override val dependencies: Seq[LinkablePackage],
                           override val components: Seq[Interface[Component]] = Seq.empty,
                         ) extends PartialPackage[ImmutablePackageBuilder] {
  override def withComponent(comp:Interface[Component]):ImmutablePackageBuilder = copy(components = components :+ comp)

  override def toLinkablePackage(placement:Directory): LinkablePackage = {

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