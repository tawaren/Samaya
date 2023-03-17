package samaya.plugin.impl.wp.dir

import samaya.compilation.ErrorManager
import samaya.plugin.impl.wp.json.WorkspaceImpl
import samaya.plugin.service.{AddressResolver, DependenciesImportSourceEncoder, LanguageCompiler, PackageEncoder, RepositoriesImportSourceEncoder, Selectors, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types
import samaya.types._
import samaya.validation.WorkspaceValidator

//A Workspace Manager for a json description of a package
class ImplicitWorkSpaceEncoder extends WorkspaceEncoder {
  override def matches(s: Selectors.WorkspaceSelector): Boolean = {
    s match {
      case Selectors.WorkspaceDecoderSelector(_ : Directory) => true
      case _ => false
    }
  }


  override def decodeWorkspace(source: GeneralSource): Option[types.Workspace] = {
    source match {
      case dir: Directory => Some(implicitWorkspace(dir))
      case _ => None
    }
  }

  def implicitWorkspace(workFolder:Directory): Workspace = {
    var includes = Set.empty[Workspace]
    var wspList = Set(workFolder.name)
    var reps = Set.empty[Repository]
    var deps = Set.empty[LinkablePackage]
    var explicits = Set.empty[Directory]
    var comps = Set.empty[Address]
    val sources = AddressResolver.listSources(workFolder)

    val (wspSources, remainingSources) = sources.partition{ source => source.extension match {
      case Some(ext) => ext.startsWith(WorkspaceEncoder.workspaceExtensionPrefix)
      case None => false
    }}

    val (repoSources, otherSources) = remainingSources.partition{ source => source.extension match {
      case Some(ext) => ext.startsWith(RepositoriesImportSourceEncoder.repositoriesExtensionPrefix)
      case None => false
    }}


    for(source <- repoSources){
      val input = AddressResolver.resolve(workFolder, Address(source), AddressResolver.InputLoader) match {
        case None => throw new Exception("Repositories could not be loaded"); //todo: error (inclusive position)
        case Some(input) => input
      }

      RepositoriesImportSourceEncoder.decodeRepositoriesSources(input) match {
        case Some(repositorySources) => reps = reps ++ repositorySources
        case None => throw new Exception("Repositories could not be loaded"); //todo: error
      }
    }

    Repository.withRepos(reps) {
      for (source <- wspSources) {
        AddressResolver.resolve(workFolder, Address(source), Workspace.Loader) match {
          case Some(value) =>
            WorkspaceValidator.validateWorkspace(value)
            explicits = explicits + value.location
            wspList = wspList + source.name
            includes = includes + value;
          case None => throw new Exception("Workspace could not be loaded"); //todo: error
        }
      }

      for (source <- otherSources) {
        source.extension match {
          case Some(ext) if ext.startsWith(DependenciesImportSourceEncoder.dependenciesExtensionPrefix) =>
            val input = AddressResolver.resolve(workFolder, Address(source), AddressResolver.InputLoader) match {
              case None => throw new Exception("Dependencies could not be loaded"); //todo: error (inclusive position)
              case Some(input) => input
            }

            DependenciesImportSourceEncoder.decodeDependenciesSources(input) match {
              case Some(dependencySources) => deps = deps ++ dependencySources
              case None => throw new Exception("Dependencies could not be loaded"); //todo: error
            }
          case Some(ext) if ext.startsWith(PackageEncoder.packageExtensionPrefix) & !wspList.contains(source.name) =>
            //Todo: Make work then incomment the guard
            val pkg = AddressResolver.resolve(workFolder, Address(source), PackageEncoder.Loader) match {
              case None => throw new Exception("Package Dependency could not be loaded"); //todo: error (inclusive position)
              case Some(input) => input
            }
            if (pkg.location != workFolder) {
              deps = deps + pkg
            }
          case Some(_) if LanguageCompiler.canCompile(source) => comps = comps + Address(source)
          case _ => //Todo: shall we do a waring if it is some but no compiler available (would warn for packages and indexes, ...)
        }
      }
      val folders = AddressResolver.listDirectories(workFolder);
      for (folder <- folders) {
        //Todo: We should not hardcode?
        if (folder.name != "out" && folder.name != "abi") {
          AddressResolver.resolveDirectory(workFolder, Address(folder)) match {
            case Some(newWorkFolder) if !explicits.contains(newWorkFolder) =>
              includes = includes + implicitWorkspace(newWorkFolder)
            case _ =>
          }
        }
      }

      //todo: allow to specify a root dir and build a parallel tree
      val out = AddressResolver.resolveDirectory(workFolder, Address(Identifier.Specific("out")), create = true)
      val inter = AddressResolver.resolveDirectory(workFolder, Address(Identifier.Specific("abi")), create = true)

      new WorkspaceImpl(
        workFolder.name,
        workFolder,
        Some(includes),
        Some(reps),
        Some(deps),
        Some(comps),
        workFolder,
        out.get,
        inter.get
      )
    }
  }
}
