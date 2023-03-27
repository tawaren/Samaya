package samaya.repository

import samaya.plugin.service.AddressResolver
import samaya.structure.types.Hash
import samaya.types.Address.ContentBased
import samaya.types.{Address, ContentAddressable, RepositoryBuilder}

import scala.collection.concurrent
import scala.reflect.ClassTag

//Todo: can we share logic with BuildRepo?
class BasicRepositoryBuilder extends RepositoryBuilder{
  //I do not like this being public but index writer need access
  protected val _repo: concurrent.Map[Hash, ContentAddressable] = concurrent.TrieMap.empty

  override def indexContent(cont: ContentAddressable): Unit = {
    _repo.put(cont.hash, cont)
  }

  override def result(): Map[Hash, ContentAddressable] = _repo.toMap
}
