package samaya.plugin.impl.repository

import samaya.plugin.service.AddressResolver
import samaya.structure.types.Hash
import samaya.types.Repository.AddressableRepository
import samaya.types.{Address, ContentAddressable, Directory, Identifier, Repository}

class MapIndexedRepository(
                            override val location:Directory,
                            override val identifier: Identifier,
                            val repo:Map[Hash,Address]
                          ) extends AddressableRepository {
  override def resolve[T <: ContentAddressable](address: Address, loader: AddressResolver.ContentLoader[T], extensionFilter:Option[Set[String]]): Option[T] = {
    address match {
      case Address.ContentBased(hash) => repo.get(hash) match {
        case Some(addr) => AddressResolver.resolve(location.resolveAddress(addr), loader)
        case None => None
      }
      case _ => None
    }
  }


  def canEqual(other: Any): Boolean = other.isInstanceOf[MapIndexedRepository]

  override def equals(other: Any): Boolean = other match {
    case that: MapIndexedRepository =>
      (that canEqual this) &&
        location == that.location &&
        identifier == that.identifier
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(location, identifier)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
