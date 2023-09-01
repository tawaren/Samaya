package samaya.plugin.impl.wp.dir

import samaya.config.ConfigValue
import samaya.plugin.config.ConfigPluginCompanion
import samaya.plugin.impl.wp.dir.ImplicitWorkSpaceEncoder.{defaultBuildDir, outputDirectory}
import samaya.plugin.impl.wp.json.WorkspaceImpl
import samaya.plugin.service.AddressResolver.Create
import samaya.plugin.service.{AddressResolver, ContentRepositoryEncoder, LanguageCompiler, PackageEncoder, ReferenceResolver, Selectors, WorkspaceEncoder}
import samaya.types
import samaya.types._

import java.io.File
import scala.collection.mutable

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
      case dir: Directory =>
        AddressResolver.resolveDirectory(outputDirectory.value.resolveAddress(WorkspaceEncoder.contextPath()), mode = Create) match {
          case Some(outputFolder) => Some(implicitWorkspace(dir, outputFolder))
          case None => AddressResolver.resolveDirectory(dir.resolveAddress(Address(defaultBuildDir.value)), mode = Create) match {
            case Some(defaultDir) => Some(implicitWorkspace(dir, defaultDir))
            case None =>
              //Todo: feedback
              println(1)
              None
          }
        }
    }
  }

  def implicitWorkspace(workFolder:Directory, outPutFolder:Directory): Workspace = {
    val discoveredSources:mutable.Map[Address,ReferenceResolver.ReferenceType] = mutable.Map.empty

    def priority(typ:ReferenceResolver.ReferenceType):Int = typ match {
      //Repository & Package are not mutually exclusive
      case ReferenceResolver.Repository => 2
      case ReferenceResolver.Package => 2
      case ReferenceResolver.Workspace => 1
      case ReferenceResolver.Source => 0
    }

    def addSource(addr:Address, typ:ReferenceResolver.ReferenceType): Unit = {
      val resolvedAddr = workFolder.resolveAddress(addr)
      discoveredSources.get(resolvedAddr) match {
        case Some(oldType) if priority(oldType) > priority(typ) =>
        case _ => discoveredSources.put(resolvedAddr,typ)
      }
    }

    def findTypes(source: GeneralSource):Set[ReferenceResolver.ReferenceType] = {
      if(ContentRepositoryEncoder.isRepository(source)){
        //Packages and Repositories are not mutually exclusive
        if(PackageEncoder.isPackage(source)) {
          Set(ReferenceResolver.Repository, ReferenceResolver.Package)
        } else {
          Set(ReferenceResolver.Repository)
        }
      } else if(PackageEncoder.isPackage(source)){
        Set(ReferenceResolver.Package)
      } else if(WorkspaceEncoder.isWorkspace(source)){
        Set(ReferenceResolver.Workspace)
      } else {
        source match {
          case source: InputSource if LanguageCompiler.canCompile(source) =>
            Set(ReferenceResolver.Source)
          case _ => Set.empty
        }
      }
    }

    //the filter prevents that iterative runs treat the output of the previous run as input
    val sources = AddressResolver.list(workFolder).filter(_.fullName != defaultBuildDir.value)
    sources.foreach{ ident =>
      val addr = workFolder.resolveAddress(Address(ident))
      AddressResolver.resolve(addr, AddressResolver.SourceLoader) match {
        case Some(source) =>
          val refs = ReferenceResolver.resolveAll(source)
          if(refs.nonEmpty) {
            refs.foreach(kv => kv._2.foreach(addSource(_,kv._1)))
          }else{
            findTypes(source).foreach(addSource(addr,_))
          }
        case None => //Todo: Unexpected errer
      }
    }

    val sourceGroups:Map[ReferenceResolver.ReferenceType,Iterable[Address]] = discoveredSources.groupMap(_._2)(_._1)

    val reps = sourceGroups.getOrElse(ReferenceResolver.Repository, Iterable.empty).flatMap{
      repoSource => AddressResolver.resolve(repoSource,Repository.Loader).asInstanceOf[Option[Repository]]
    }.toSet

    Repository.withRepos(reps){
      val deps = sourceGroups.getOrElse(ReferenceResolver.Package, Iterable.empty).flatMap{
        repoSource => AddressResolver.resolve(repoSource,PackageEncoder.Loader)
      }.toSet

      val includes = sourceGroups.getOrElse(ReferenceResolver.Workspace, Iterable.empty).flatMap{
        repoSource => AddressResolver.resolve(repoSource,Workspace.Loader)
      }.toSet

      val comps = sourceGroups.getOrElse(ReferenceResolver.Source, Set.empty).toSet

      val out = AddressResolver.resolveDirectory(outPutFolder.resolveAddress(Address(Identifier.Specific("out"))), mode = Create)
      val inter = AddressResolver.resolveDirectory(outPutFolder.resolveAddress(Address(Identifier.Specific("abi"))), mode = Create)

      new WorkspaceImpl(
        workFolder.identifier.name,
        workFolder,
        outPutFolder,
        includes,
        reps,
        deps,
        comps,
        workFolder,
        out.get,
        inter.get
      )
    }
  }
}

object ImplicitWorkSpaceEncoder extends ConfigPluginCompanion{

  val defaultBuildDir : ConfigValue[String] = arg("fallback_build_dir").default("build")
  val outputDirectory : ConfigValue[Directory] = arg("output|out|o")
    .default(defaultBuildDir.value+File.separator+"packages")
    .flatMap{outputPath =>
      AddressResolver.provideDefault() match {
        case Some(base) => AddressResolver.resolveDirectory(base.resolveAddress(AddressResolver.parsePath(outputPath).get), mode = Create)
        case None =>  AddressResolver.resolveDirectory(AddressResolver.parsePath(outputPath).get, mode = Create)
      }
    }
}
