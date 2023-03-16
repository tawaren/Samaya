package samaya.plugin.impl.repository

import ky.korins.blake3.Blake3
import samaya.plugin.service.AddressResolver
import samaya.plugin.shared.repositories.Repositories.Repository
import samaya.structure.ContentAddressable
import samaya.structure.types.Hash
import samaya.types.{Address, Directory, Identifier}

class MapIndexedRepository(
                            override val location:Directory,
                            override val identifier: Identifier,
                            val repo:Map[Hash,Address]
                          ) extends Repository {
  override def resolve[T <: ContentAddressable](address: Address, loader: AddressResolver.Loader[T]): Option[T] = {
    address match {
      case Address.ContentBased(hash) => repo.get(hash) match {
        case Some(addr) => AddressResolver.resolve(location,addr,loader)
        case None => None
      }
      case _ => None
    }
  }

  override lazy val hash: Hash = {
    val digest = Blake3.newHasher()
    repo.keySet.foreach(h => {
      digest.update(h.data)
    })
    Hash.fromBytes(digest.done(Hash.byteLen))
  }

}
