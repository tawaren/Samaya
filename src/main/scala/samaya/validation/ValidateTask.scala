package samaya.validation

import samaya.ProjectUtils
import samaya.ProjectUtils.{getContextRepo, processDependencies, processWithArgRepos, traverseDependencies}
import samaya.compilation.ErrorManager.{Checking, Error, PlainMessage, feedback}
import samaya.config.ConfigValue
import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.pakage.PackageTask.collectArg
import samaya.plugin.config.{ConfigPluginCompanion, RawPluginCompanion}
import samaya.plugin.service._
import samaya.structure.LinkablePackage
import samaya.types._
import samaya.validation.ValidateTask.{TaskName, mode, repos, target}

import scala.collection.mutable
import scala.reflect.ClassTag


class ValidateTask extends TaskExecutor {

  override def matches(s: Selectors.TaskExecutorSelector): Boolean = s match {
    case Selectors.SelectByName(TaskName) => true
    case _ => false
  }

  override def execute(name: String): Unit = validate(target.value, mode.value)
  override def apply[S: ClassTag, T: ClassTag](name: String, src: S): Option[T] = None

  def validate(target:String, mode: ProcessingMode):Option[LinkablePackage] = {
    processWithArgRepos(repos.value) {
      //todo: have a multi location selector
      val parent: Directory = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))

      val ident = AddressResolver.parsePath(target) match {
        case Some(id) => id
        case None => throw new Exception("Illegal arg"); //todo: error
      }

      AddressResolver.resolve(parent.resolveAddress(ident), PackageEncoder.Loader) match {
        case None =>
          feedback(PlainMessage("Package not found: " + parent.resolveAddress(ident), Error, Checking()))
          None
        case Some(pkg) => ProjectUtils.processWitContextRepo(pkg) {
          val t0 = System.currentTimeMillis()
          validateAll(pkg, mode)
          println(s"validation finished in ${System.currentTimeMillis() - t0} ms")
          Some(pkg)
        }
      }
    }
  }

  //Todo: I wish we could fetch repos in parallel
  //      But it would require a double pass
  //      1. Fetch pkg -> Repos (in parallel)
  //      2. Join all repos on the upstream path
  //  Do it later: has no priority
  def collectPackages(pkg:LinkablePackage, activeRepos: Set[Repository], packageCollector: mutable.Map[LinkablePackage,Set[Repository]],  mode: ProcessingMode):Unit = {
    val updatedRepos = activeRepos ++ getContextRepo(pkg)
    packageCollector.updateWith(pkg)(_.map(_++updatedRepos).orElse(Some(updatedRepos)))
    val includes = mode match {
      case Deep => pkg.dependencies.map(_.name).toSet
      case Fresh => pkg.includes.getOrElse(Set.empty)
      case Shallow => Set.empty[String]
    }
    //we go recursive even if we already have it, to ensure we collect the repos of all paths
    // todo: repo collecting may give false assurance - but otherwise we need to validate multiple times
    //    at least it is consistent with build & we can make it consistent with deploy
    pkg.dependencies.filter(p => includes.contains(p.name)).foreach{ p =>
      collectPackages(p, updatedRepos,packageCollector, mode)
    }
  }

  def validateAll(pkg: LinkablePackage, mode: ProcessingMode): Unit ={
    //We use a object identity based approach here instead if a hash based
    // The reason is that we want to validate even non-hash relevant aspects
    // For example mapped sources, as a developer may inspect them
    //  We do not want to miss a package with the same hash but different sources than another
    val packageCollector: mutable.Map[LinkablePackage,Set[Repository]] = mutable.Map.empty
    collectPackages(pkg,Set.empty, packageCollector,mode)
    val tasks = packageCollector.map {
      case (p,r) => IndependentJob[Unit](() => validateOne(p,r))
    }

    JobExecutor.executeIndependentJobs(JobResultBuilder(),tasks)
  }

  def validateOne(pkg: LinkablePackage, activeRepos: Set[Repository]):Unit = {
    val t0 = System.currentTimeMillis()
    Repository.withRepos(activeRepos) {
      PackageValidator.validatePackage(pkg)
    }
    println(s"validation of package ${pkg.name} finished in ${System.currentTimeMillis()-t0} ms" )
  }
}

//Todo: Add deep Validation etc. like in Deploy and Package
object ValidateTask extends ConfigPluginCompanion {

  val TaskName: String = "validate"

  val target: ConfigValue[String] =  param(1).default("/")
  val mode: ConfigValue[ProcessingMode] = select(
    "r|d|recursive|deep" -> Deep,
    "t|s|top|shallow" -> Shallow
  ).default(Fresh)

  private val repos : ConfigValue[Seq[String]] = collectArg("validate.repos|validate.repositories|repositories|repos").default(Seq.empty)


}
