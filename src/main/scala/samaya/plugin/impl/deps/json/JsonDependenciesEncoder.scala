package samaya.plugin.impl.deps.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import samaya.plugin.impl.deps.json.JsonModel._
import samaya.plugin.service.DependenciesEncoder.DependenciesExtension
import samaya.plugin.service.{AddressResolver, DependenciesEncoder, PackageEncoder, Selectors}
import samaya.structure.LinkablePackage
import samaya.types.{InputSource, Directory}

//A Dependency Manager for a json description of a dependency list
class JsonDependenciesEncoder extends DependenciesEncoder {

  val Json = "json"
  override def matches(s: Selectors.DependenciesSelector): Boolean = {
    s match {
      case Selectors.DependenciesDeserializationSelector(DependenciesExtension(Json)) => true
      case _ => false
    }
  }

  override def deserializeDependenciesSources(file: InputSource): Option[Seq[LinkablePackage]] = {
    val paths = readFromStream[Seq[String]](file.content)
    val workspaceLocation: Directory = file.location
    Some(paths
      .flatMap(AddressResolver.parsePath)
      .flatMap(l =>  AddressResolver.resolve(workspaceLocation,l, PackageEncoder.Loader, Some(Set(PackageEncoder.packageExtensionPrefix))))
    )
  }
}
