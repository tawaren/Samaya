package samaya.plugin.impl.repository

import samaya.plugin.service.{AddressResolver, ContentRepositoryLoader, Selectors}
import samaya.plugin.shared.repositories.Repositories
import samaya.types.Address.HybridAddress
import samaya.types.InputSource

import scala.io.Source

class DefaultFileRepositoryLoader extends ContentRepositoryLoader{

  override def matches(s: Selectors.RepositoryLoaderSelector): Boolean = s match {
    case Selectors.LoadRepository(source) => source.identifier.extension.contains("repo")
  }

  override def loadRepository(source:InputSource): Option[Repositories.Repository] = {
    val root = source.location
    val repo = Source.fromInputStream(source.content).getLines().flatMap(AddressResolver.parsePath).flatMap{
      case HybridAddress(target, loc) => Some((target.target, loc))
      case _ => None //Todo: Error <-- also an error in the previous flat map
    }
    Some(new MapIndexedRepository(root, source.identifier, repo.toMap))
  }
}
