package samaya.plugin.impl.wp.json

import com.github.plokhotnyuk.jsoniter_scala.core._
import JsonModel._
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager._
import samaya.plugin.config.ConfigPluginCompanion
import samaya.plugin.service.AddressResolver.Create
import samaya.plugin.service.{AddressResolver, PackageEncoder, Selectors, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types
import samaya.types.{Address, Addressable, Directory, GeneralSource, InputSource, Repository}

//A Workspace Manager for a json description of a package
class JsonWorkspaceEncoder extends WorkspaceEncoder {

  val Ext = WorkspaceEncoder.workspaceExtensionPrefix+".json"
  override def matches(s: Selectors.WorkspaceSelector): Boolean = s match {
      case Selectors.WorkspaceDecoderSelector(input : InputSource) => input.identifier.extension.contains(Ext)
      case _ => false
  }

  var cycleBreaker:Set[(Directory,String)] = Set.empty


  override def decodeWorkspace(source: GeneralSource): Option[types.Workspace] = {
    val file = source match {
      case source: InputSource => source
      case _ => return None
    }
    //parse the package with Package as description
    val parsed = file.read(readFromStream[Workspace](_))
    val name: String = parsed.name
    val workspaceLocation: Directory = file.location

    def loadFromPaths[T <: Addressable](paths:Option[Seq[String]], loader:AddressResolver.Loader[T]): Option[Set[T]] = {
      paths.map(p => p
        .toSet[String]
        .flatMap(AddressResolver.parsePath)
        .flatMap((path: Address) => AddressResolver.resolve(workspaceLocation.resolveAddress(path), loader))
      )
    }

    //Todo: Error when fails
    //Todo: Is None right
    val repos = loadFromPaths(parsed.repositories, Repository.Loader).getOrElse(Set.empty).asInstanceOf[Set[Repository]]

    Repository.withRepos(repos){
        val oldBreaker = cycleBreaker
        val (includes: Set[types.Workspace], dependencies: Set[LinkablePackage]) = if(!oldBreaker.contains((workspaceLocation, name))) {
          cycleBreaker = oldBreaker + ((workspaceLocation, name))
          val includes: Set[types.Workspace] = loadFromPaths(parsed.includes, types.Workspace.Loader).getOrElse(Set.empty)

          if(includes.size != parsed.includes.getOrElse(Set.empty).size) {
            feedback(PlainMessage(s"Could not deserialize all includes", Warning, Decoding()))
          }

          val dependencies: Set[LinkablePackage] = loadFromPaths(parsed.dependencies, PackageEncoder.Loader).getOrElse(Set.empty)
          if(dependencies.size != parsed.dependencies.getOrElse(Set.empty).size) {
            feedback(PlainMessage(s"Could not deserialize all dependencies", Warning, Decoding()))
          }
          cycleBreaker = oldBreaker
          (includes, dependencies)
        } else {
          feedback(PlainMessage(s"Cyclic dependencies are not allowed. $name depends on itself", Error, Decoding()))
          (Set.empty, Set.empty)
        }


        //todo: if something gets lost we ned an error
        val components: Set[Address] = parsed.sources.getOrElse(Set.empty).flatMap(AddressResolver.parsePath).toSet

        val codeIdent = AddressResolver.parsePath(parsed.locations.code)match {
          case Some(id) => id
          case None =>
            ErrorManager.feedback(PlainMessage("Could not parse code path", ErrorManager.Error, Decoding()))
            return None
        }

        val interfaceIdent = AddressResolver.parsePath(parsed.locations.interface)match {
          case Some(id) => id
          case None =>
            ErrorManager.feedback(PlainMessage("Could not parse interface path", ErrorManager.Error, Decoding()))
            return None
        }

        val packageIdent = AddressResolver.parsePath(parsed.target) match {
          case Some(id) => id
          case None =>
            ErrorManager.feedback(PlainMessage("Could not parse package path", ErrorManager.Error, Decoding()))
            return None
        }

        val sourceLocation: Directory = parsed.locations.source match {
          case None => workspaceLocation
          case Some(path) =>
            val sourceIdent = AddressResolver.parsePath(path) match {
              case Some(id) => id
              case None =>
                ErrorManager.feedback(PlainMessage("Could not parse source path", ErrorManager.Error, Decoding()))
                return None

            }

            AddressResolver.resolveDirectory(workspaceLocation.resolveAddress(sourceIdent)) match {
              case Some(loc) => loc
              case None =>
                ErrorManager.feedback(PlainMessage("Could not load source location", ErrorManager.Error, Decoding()))
                return None
            }
        }

        val packageLocation = AddressResolver.resolveDirectory(workspaceLocation.resolveAddress(packageIdent), mode = Create)  match {
          case Some(loc) => loc
          case None =>
            ErrorManager.feedback(PlainMessage("Could not load interface location", ErrorManager.Error, Decoding()))
            return None
        }

        val codeLocation: Directory = AddressResolver.resolveDirectory(packageLocation.resolveAddress(codeIdent), mode = Create)  match {
          case Some(loc) => loc
          case None =>
            ErrorManager.feedback(PlainMessage("Could not load code location", ErrorManager.Error, Decoding()))
            return None
        }

        val interfaceLocation: Directory = AddressResolver.resolveDirectory(packageLocation.resolveAddress(interfaceIdent), mode = Create)  match {
          case Some(loc) => loc
          case None =>
            ErrorManager.feedback(PlainMessage("Could not load interface location", ErrorManager.Error, Decoding()))
            return None
        }

        val workspace =  new WorkspaceImpl(name, workspaceLocation, packageLocation, includes, repos, dependencies, components, sourceLocation, codeLocation, interfaceLocation)
        Some(workspace)
      }
    }
}

object JsonWorkspaceEncoder extends ConfigPluginCompanion {}
