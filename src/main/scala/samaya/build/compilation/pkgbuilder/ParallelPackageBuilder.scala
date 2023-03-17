package samaya.build.compilation.pkgbuilder

import ky.korins.blake3.Blake3
import samaya.build.PartialPackage
import samaya.structure.types.Hash
import samaya.structure.{Component, Interface, LinkablePackage}
import samaya.types.Directory



case class ParallelPackageBuilder(
                                   override val dependencies: Seq[LinkablePackage],
                                   override val name: String,
                                   comps: Set[Interface[Component]] = Set.empty,
                                 ) extends PartialPackage[ParallelPackageBuilder] {

  override lazy val components: Seq[Interface[Component]] = comps.toSeq

  override def withComponent(comp:Interface[Component]):ParallelPackageBuilder = {
    copy(comps = comps + comp)
  }

  def merge(other:ParallelPackageBuilder):ParallelPackageBuilder = {
    copy(comps = comps ++ other.comps)
  }

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