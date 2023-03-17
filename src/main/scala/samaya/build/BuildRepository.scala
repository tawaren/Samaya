package samaya.build

import samaya.plugin.service.AddressResolver
import samaya.structure.types.Hash
import samaya.types.Address.ContentBased
import samaya.types.{Address, ContentAddressable, Repository}

import scala.collection.concurrent
import scala.reflect.ClassTag

object BuildRepository extends Repository{
  //I do not like this being public but index writer need access
  private val buildRepo: concurrent.Map[Hash, ContentAddressable] = concurrent.TrieMap.empty

  def addEntry(cont:ContentAddressable):Unit = {
    buildRepo.put(cont.hash, cont)
  }

  def repo:Map[Hash, ContentAddressable] = buildRepo.toMap

  def ensureExtension[T <: ContentAddressable](res:T, extensionFilter: Option[Set[String]]):Boolean = {
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
      case ContentBased(hash) => BuildRepository.buildRepo.get(hash) match {
        case Some(content : T) if ensureExtension(content, extensionFilter) => Some(content)
        case _ => None
      }
      case _ => None
    }
  }


}
