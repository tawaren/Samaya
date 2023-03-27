package samaya.pakage

import samaya.ProjectUtils.{buildIfMissing, traverseDependencies}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.unexpected
import samaya.pakage.PackageTask.{TaskName, mode, source, target}
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.service.AddressResolver.{Create, ReCreate}
import samaya.plugin.service.TaskExecutor.IsClass
import samaya.plugin.service.{AddressResolver, ContentRepositoryEncoder, JobExecutor, PackageEncoder, Selectors, TaskExecutor}
import samaya.repository.BasicRepositoryBuilder
import samaya.structure.LinkablePackage
import samaya.types.{Address, Deep, Directory, Fresh, Identifier, InputSource, ProcessingMode, RepositoryBuilder, Shallow, Workspace}

import scala.collection.mutable
import scala.reflect.ClassTag

class PackageTask extends TaskExecutor{

  private val IsString = IsClass[String]
  private val IsDirectory = IsClass[Directory]

  override def matches(s: Selectors.TaskExecutorSelector): Boolean = s match {
    case Selectors.SelectByName(TaskName) => true
    case Selectors.SelectApplyTask(TaskName, IsString(), IsDirectory()) => true
    case _ => false
  }
  override def execute(name: String): Unit = packaging(source.value, target.value, mode.value)

  override def apply[S: ClassTag, T: ClassTag](_name: String, src: S): Option[T] = src match {
    case IsString(source) => IsDirectory[T](packaging(source, target.value, mode.value))
  }

  def packaging(source:String, target:String, mode: ProcessingMode): Option[Directory] = {
    val parent = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))
    val targetPath = AddressResolver.parsePath(target).flatMap(p => {
      AddressResolver.resolveDirectory(parent.resolveAddress(p), mode = Create)
    }).getOrElse(throw new Exception("Illegal arg"))

    buildIfMissing(source, "Skipped packaging due to compilation error"){lp =>
      val t0 = System.currentTimeMillis()
      val packagePath = AddressResolver.resolveDirectory(targetPath.resolveAddress(Address(lp.name, "sar")), mode = ReCreate).getOrElse(throw new Exception("Illegal arg"))
      val repoBuilder = new BasicRepositoryBuilder()
      packagePackage(lp, packagePath, packagePath, mode, repoBuilder)
      //Todo: switch the indexing mechanism
      //      maybe have an identifier in repoBuilder
      //      or completely replace old index system
      ContentRepositoryEncoder.storeRepository(packagePath,repoBuilder)
      println(s"packaging of ${packagePath} finished in ${System.currentTimeMillis()-t0} ms" )
      packagePath
    }
  }

  //Todo: Deduplicate packages if they appear more than once in the graph
  private def navigateAndPackage(lp:LinkablePackage, parentDir:Directory, rootDir:Directory,  mode:ProcessingMode, repoBuilder:RepositoryBuilder):LinkablePackage = {
    AddressResolver.resolveDirectory(parentDir.resolveAddress(Address(lp.name)), mode = Create) match {
      case Some(packagePath) => packagePackage(lp,packagePath, rootDir,mode, repoBuilder)
      case None => unexpected("Can not generate package location", ErrorManager.Packaging())
    }
  }

  private def packagePackage(lp:LinkablePackage, pkgDir:Directory, rootDir:Directory, mode:ProcessingMode, repoBuilder:RepositoryBuilder):LinkablePackage = {
    //package all dependencies
    val adaptedDependencies = traverseDependencies(lp,mode){dep =>
      navigateAndPackage(dep,pkgDir, rootDir, mode, repoBuilder)
    }

    val t0 = System.currentTimeMillis()

    val srcDir = AddressResolver.resolveDirectory(pkgDir.resolveAddress(Address("src")), mode = Create) match {
      case Some(dir) => dir
      case None => ??? //Todo: warning or error
    }

    val abiDir = AddressResolver.resolveDirectory(pkgDir.resolveAddress(Address("abi")), mode = Create) match {
      case Some(dir) => dir
      case None => ??? //Todo: warning or error
    }

    val codeDir = AddressResolver.resolveDirectory(pkgDir.resolveAddress(Address("code")), mode = Create) match {
      case Some(dir) => dir
      case None => ??? //Todo: warning or error
    }

    //This is necessary as a workspace could
    // import multiple sources with the same name
    //  from different locations
    // However: Because 1 source can generate multiple components
    //          the same source can appear more than once
    //          but only needs to be copied once
    // Alternative would be per hash to
    //  deduplicate same sources from different locations as well
    val srcs = mutable.Map.empty[Identifier,Set[InputSource]]

    def copy(src:InputSource, dir:Directory, mangle: Boolean = false):Option[CopyJob] = {
      val trgIdent = src.identifier match {
        case spec : Identifier.Specific => if(mangle) {
          srcs.get(spec) match {
            case Some(inputs) if inputs.contains(src) => return None
            case Some(inputs) =>
              srcs.put(spec,inputs + src)
              spec.copy(name = spec.name+"_"+inputs.size)
            case None =>
              srcs.put(spec, Set(src))
              spec
          }
        } else {
          spec
        }
        case Identifier.General(_, _) => ??? //Todo: can we have a fallback
      }

      AddressResolver.resolveSink(dir, trgIdent) match {
        case Some(sink) => Some(new CopyJob(src,sink))
        case None => None//Todo: warning or error
      }
    }

    val copyTasks = lp.components.flatMap{comp =>
      val res = Seq.newBuilder[CopyJob]
      res.addAll(comp.meta.code.flatMap(copy(_, codeDir)))
      res.addAll(comp.meta.interface.flatMap(copy(_, abiDir)))
      res.addAll(comp.meta.sourceCode.flatMap(copy(_, srcDir, mangle = true)))
      res.result()
    }

    JobExecutor.executeIndependentJobs(new CopyJob.IndexContent(repoBuilder), copyTasks)

    //discard all sources only content shall remain (will be content addressed anyway)
    val adaptedComponents = lp.components.map(c => c.toInterface(c.meta.copy(interface = None, code = None, sourceCode = None)))

    //we adapt to include exactly what we included
    val includes = mode match {
      //include everything
      case Deep => Some(adaptedDependencies.map(_.name).toSet)
      //include same things as source
      case Fresh => lp.includes
      //Include Nothing
      case Shallow => None
    }

    val newPkg = new LinkablePackage(
      lp.interfacesOnly,
      pkgDir,
      lp.hash,
      lp.name,
      adaptedComponents,
      adaptedDependencies,
      includes
    )

    PackageEncoder.serializePackage(newPkg)
    repoBuilder.indexContent(newPkg)

    println(s"packaging of package ${lp.name} finished in ${System.currentTimeMillis()-t0} ms" )

    newPkg
  }
}

object PackageTask extends ConfigPluginCompanion {

  val TaskName: String = "package"

  val source: ConfigValue[String] =  param(1).default("/")
  val target: ConfigValue[String] =  param(2).default("/build/lib")
  val mode: ConfigValue[ProcessingMode] = select(
    "r|d|recursive|deep" -> Deep,
    "t|s|top|shallow" -> Shallow
  ).default(Fresh)
}