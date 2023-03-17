package samaya.plugin.impl.deps.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import samaya.plugin.impl.deps.json.JsonModel._
import samaya.plugin.service.{AddressResolver, DependenciesImportSourceEncoder, PackageEncoder, Selectors}
import samaya.structure.LinkablePackage
import samaya.types.{Directory, GeneralSource, InputSource}

//A Dependency Manager for a json description of a dependency list
class JsonDependenciesImportSourceEncoder extends DependenciesImportSourceEncoder {

  private val ext = DependenciesImportSourceEncoder.dependenciesExtensionPrefix+".json"
  override def matches(s: Selectors.DependenciesImportSelector): Boolean = {
    s match {
      case Selectors.DependenciesDecoderSelector(input : InputSource) => input.identifier.extension.contains(ext)
      case _ => false
    }
  }

  override def decodeDependenciesSources(source : GeneralSource): Option[Seq[LinkablePackage]] = {
    val file = source match {
      case source: InputSource => source
      case _ => return None
    }
    val paths = readFromStream[Seq[String]](file.content)
    val workspaceLocation: Directory = file.location
    Some(paths
      .flatMap(AddressResolver.parsePath)
      .flatMap(l =>  AddressResolver.resolve(workspaceLocation, l, PackageEncoder.Loader))
    )
  }
}
