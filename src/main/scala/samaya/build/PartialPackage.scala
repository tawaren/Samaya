package samaya.build

import java.security.MessageDigest

import com.rfksystems.blake2b.Blake2b
import samaya.structure.types.Hash
import samaya.structure.{Component, LinkablePackage, Interface, ModuleInterface, TransactionInterface, Module, Package, Transaction}
import samaya.types.Location
case class PartialPackage(
                           override val name: String,
                           override val dependencies: Seq[LinkablePackage],
                           override val components: Seq[Interface[Component]] = Seq.empty,
) extends Package{
  def withComponent(comp:Interface[Component]):PartialPackage = copy(components = components :+ comp)

  def toLinkablePackage(placement:Location): LinkablePackage = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
      components.map(e => e.meta.interfaceHash).sorted.distinct.foreach(h => digest.update(h.data))
      dependencies.sortBy(p => p.name).distinct.foreach(p => {
        digest.update(p.name.getBytes)
        digest.update(p.hash.data)
      })

    new LinkablePackage(
      false,
      placement,
      Hash.fromBytes(digest.digest()),
      name,
      components,
      dependencies,
    )
  }
}
