package samaya.repository

import samaya.plugin.service.AddressResolver
import samaya.structure.types.Hash
import samaya.types.Address.ContentBased
import samaya.types.{Address, ContentAddressable, Repository, RepositoryBuilder}

import scala.collection.concurrent
import scala.reflect.ClassTag

object BuildRepository extends BasicRepositoryBuilder with Repository {

  def ensureExtension[T <: ContentAddressable](res: T, extensionFilter: Option[Set[String]]): Boolean = {
    extensionFilter match {
      case Some(filter) => res.identifier.extension match {
        case Some(ext) => filter.contains(ext)
        case None => false
      }
      case None => true
    }
  }

  override def resolve[T <: ContentAddressable](address: Address, loader: AddressResolver.ContentLoader[T], extensionFilter: Option[Set[String]]): Option[T] = {
    implicit val classTag: ClassTag[T] = loader.tag
    address match {
      case ContentBased(hash) => _repo.get(hash) match {
        case Some(content: T) if ensureExtension(content, extensionFilter) => Some(content)
        case _ => None
      }
      case _ => None
    }
  }


}
