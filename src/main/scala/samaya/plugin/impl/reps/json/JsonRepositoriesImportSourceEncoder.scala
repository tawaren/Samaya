package samaya.plugin.impl.reps.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import samaya.plugin.impl.deps.json.JsonModel._
import samaya.plugin.service.{AddressResolver, RepositoriesImportSourceEncoder, Selectors}
import samaya.types.{Address, Directory, GeneralSource, InputSource, Repository}

//A Dependency Manager for a json description of a dependency list
class JsonRepositoriesImportSourceEncoder extends RepositoriesImportSourceEncoder {

  private val ext = RepositoriesImportSourceEncoder.repositoriesExtensionPrefix+".json"
  override def matches(s: Selectors.RepositoriesImportSelector): Boolean = {
    s match {
      case Selectors.RepositoriesDecoderSelector(input : InputSource) => input.identifier.extension.contains(ext)
      case _ => false
    }
  }

  override def decodeRepositoriesSources(source: GeneralSource): Option[Seq[Repository]] = {
    val file = source match {
      case source: InputSource => source
      case _ => return None
    }
    val paths = readFromStream[Seq[String]](file.content)
    val workspaceLocation: Directory = file.location
    Some(paths
      .flatMap(AddressResolver.parsePath)
      .flatMap((path: Address) => AddressResolver.resolve(workspaceLocation, path, Repository.Loader))
    )
  }
}
