package samaya.plugin.impl.repository

import samaya.plugin.service.{AddressResolver, ContentRepositoryLoader, Selectors}
import samaya.types.Address.HybridAddress
import samaya.types.Repository.AddressableRepository
import samaya.types.{Directory, GeneralSource, InputSource, Repository}

import scala.io.Source

class DefaultFileRepositoryLoader extends ContentRepositoryLoader{

  override def matches(s: Selectors.RepositoryLoaderSelector): Boolean = s match {
    case Selectors.LoadRepository(source : InputSource) => source.identifier.extension.contains("repo")
  }

  override def loadRepository(generalSource:GeneralSource): Option[AddressableRepository] = {
    val source = generalSource match {
      case source: InputSource => source
      case _ => return None;
    }

    val root = source.location
    val repo = Source.fromInputStream(source.content).getLines().flatMap(AddressResolver.parsePath).flatMap{
      case HybridAddress(target, loc) => Some((target.target, loc))
      case _ => None //Todo: Error <-- also an error in the previous flat map
    }
    Some(new MapIndexedRepository(root, source.identifier, repo.toMap))
  }
}
