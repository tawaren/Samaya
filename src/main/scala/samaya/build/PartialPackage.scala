package samaya.build

import ky.korins.blake3.Blake3
import samaya.structure.types.{CompLink, Hash}
import samaya.structure.{Component, Interface, LinkablePackage, Package}
import samaya.types.Directory

case class PartialPackage(
                           override val name: String,
                           override val dependencies: Seq[LinkablePackage],
                           override val components: Seq[Interface[Component]] = Seq.empty,
                           comps: Set[CompLink] = Set.empty,
                         ) extends Package {

  private def mergeInto(from:PartialPackage, into:PartialPackage):PartialPackage = {
    var seq:Seq[Interface[Component]] = into.components
    var set:Set[CompLink] = into.comps
    for(c <- from.components){
      if(!set.contains(c.link)){
        seq = seq :+ c
        set = set + c.link
      }
    }
    into.copy(comps = set, components = seq)
  }

  def merge(other: PartialPackage): PartialPackage = {
    if(components.size > other.components.size){
      mergeInto(other, this)
    } else {
      mergeInto(this, other)
    }
  }

  def withComponent(comp: Interface[Component]): PartialPackage = {
    copy(comps = comps + comp.link, components = components :+ comp)
  }

  def toLinkablePackage(placement: Directory,includes:Set[String]): LinkablePackage = {
    val digest = Blake3.newHasher()
    //Note: we sort only for the hash not for the package to preserve deployment order
    components.map(e => e.meta.interfaceHash).sorted.foreach(h => digest.update(h.data))
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
      Some(includes),
    )
  }
}
