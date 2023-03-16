package samaya.plugin.shared.index

import samaya.structure.ContentAddressable
import samaya.structure.types.Hash

import scala.collection.mutable

object CachedRepository {
  val repo: mutable.Map[Hash, ContentAddressable] = mutable.Map.empty
}
