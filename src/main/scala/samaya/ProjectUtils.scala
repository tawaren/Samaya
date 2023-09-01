package samaya

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Always, Info, PlainMessage, canProduceErrors, feedback, producesErrorValue}
import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.plugin.service.{AddressResolver, JobExecutor, PackageEncoder, TaskExecutor}
import samaya.repository.BuildRepository
import samaya.structure.LinkablePackage
import samaya.types.{Address, Deep, Directory, Fresh, ProcessingMode, Repository, Shallow, Workspace}
import samaya.validation.WorkspaceValidator

object ProjectUtils {

  def createWorkspace(target: String): Workspace = {
    //todo: have a multi location selector
    val parent: Directory = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = AddressResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg"); //todo: error
    }

    AddressResolver.resolve(parent.resolveAddress(ident), Workspace.Loader) match {
      case None => throw new Exception("Workspace not found");
      case Some(value) => value
    }
  }

  def getContextRepo(pkg:LinkablePackage):Set[Repository] = {
    AddressResolver.resolve(pkg.location.resolveAddress(Address()), Repository.Loader) match {
      case Some(repo) => Set(repo)
      case _ => Set.empty
    }
  }

  def processWitContextRepo[R](pkg:LinkablePackage)(f: => R): R = {
    Repository.withRepos(getContextRepo(pkg))(f)
  }


  def processWithArgRepos[R](repos:Seq[String])(f: => R): R = {
    val parent = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))
    //Todo: Error instead of fail
    val repoAddrs = repos.flatMap(AddressResolver.parsePath)
    val resReps = repoAddrs.flatMap(a => AddressResolver.resolve(parent.resolveAddress(a), Repository.SilentLoader)).map(r => r:Repository).toSet
    Repository.withRepos(resReps)(f)
  }

  def processWitContextRepo[R](dir:Directory)(f: => R): R = {
    AddressResolver.resolve(dir.resolveAddress(Address()), Repository.SilentLoader) match {
      case Some(repo) => Repository.withRepos(Set(repo))(f)
      case _ => f
    }
  }

  def buildThenProcess[R](source:String, onError: => String)(f: LinkablePackage => R):Option[R] = {
    ErrorManager.producesErrorValue(TaskExecutor[String,LinkablePackage]("build", source)) match {
      case Some(Some(lp)) => Some(Repository.withRepos(Set(BuildRepository))(f(lp)))
      case _ =>
        feedback(PlainMessage(onError, Info, Always))
        None
    }
  }

  def buildIfMissing[R](source:String, onError: => String)(f: LinkablePackage => R):Option[R] = {
    //Todo: Better errors
    val parent = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))
    val sourcePath = AddressResolver.parsePath(source).getOrElse(throw new Exception("Illegal arg"))

    AddressResolver.resolve(parent.resolveAddress(sourcePath), PackageEncoder.Loader) match {
      case None =>
        feedback(PlainMessage("Target was not a package - trying to treat it as workspace", Info, Always))
        buildThenProcess(source,onError)(f)
      case Some(lp) =>
        Some(ProjectUtils.processWitContextRepo(lp)(f(lp)))
    }
  }

  def traverseDependencies(pkg:LinkablePackage, mode:ProcessingMode)(f: LinkablePackage => LinkablePackage):Seq[LinkablePackage] = {
    val deep = mode match {
      case Deep => true
      case Fresh => false
      case Shallow => return pkg.dependencies
    }
    val deps = pkg.dependencies
    val includes = pkg.includes.getOrElse(Set.empty)
    val resultBuilder = Seq.newBuilder[LinkablePackage]
    resultBuilder.sizeHint(deps.size)

    val tasks = deps.map(IndependentJob({ dep =>
      if(!deep && !includes.contains(dep.name)){
        dep
      } else {
        if(includes.contains(dep.name)){
          f(dep)
        } else {
          processWitContextRepo(dep)(f(dep))
        }
      }
    }))

    JobExecutor.executeIndependentJobs(JobResultBuilder(resultBuilder.addOne), tasks)
    resultBuilder.result()
  }

  //Todo: Unecessarily builds a result collection but ok for now
  def processDependencies(pkg:LinkablePackage, mode:ProcessingMode)(f: LinkablePackage => Unit):Unit = {
    traverseDependencies(pkg, mode){ pkg =>
      f(pkg)
      pkg
    }
  }
}
