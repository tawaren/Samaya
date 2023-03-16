package samaya.plugin.impl.reps.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import samaya.plugin.impl.deps.json.JsonModel._
import samaya.plugin.service.DependenciesImportSourceEncoder.DependenciesExtension
import samaya.plugin.service.{AddressResolver, ContentRepositoryLoader, DependenciesImportSourceEncoder, PackageEncoder, RepositoriesImportSourceEncoder, Selectors}
import samaya.plugin.shared.repositories.Repositories.Repository
import samaya.structure.LinkablePackage
import samaya.types.{Directory, InputSource}

//A Dependency Manager for a json description of a dependency list
class JsonRepositoriesImportSourceEncoder extends RepositoriesImportSourceEncoder {

  val Json = "json"
  override def matches(s: Selectors.RepositoriesImportSelector): Boolean = {
    s match {
      case Selectors.RepositoriesDeserializationSelector(DependenciesExtension(Json)) => true
      case _ => false
    }
  }

  override def deserializeRepositoriesSources(file: InputSource): Option[Seq[Repository]] = {
    val paths = readFromStream[Seq[String]](file.content)
    val workspaceLocation: Directory = file.location
    Some(paths
      .flatMap(AddressResolver.parsePath)
      .flatMap(l => AddressResolver.resolve(workspaceLocation,l, ContentRepositoryLoader.Loader, None))
    )
  }
}
