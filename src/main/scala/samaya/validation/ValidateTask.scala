package samaya.validation

import samaya.ProjectUtils
import samaya.ProjectUtils.{processDependencies, traverseDependencies}
import samaya.compilation.ErrorManager.{Checking, Error, PlainMessage, feedback}
import samaya.jobs.{IndependentJob, JobResultBuilder}
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue, RawPluginCompanion}
import samaya.plugin.service._
import samaya.structure.LinkablePackage
import samaya.types._
import samaya.validation.ValidateTask.{TaskName, mode, target}

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
    //todo: have a multi location selector
    val parent:Directory = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = AddressResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    AddressResolver.resolve(parent.resolveAddress(ident), PackageEncoder.Loader) match {
      case None =>
        feedback(PlainMessage("Package not found: "+parent.resolveAddress(ident), Error, Checking()))
        None
      case Some(pkg) => ProjectUtils.processWitContextRepo(pkg){
        val t0 = System.currentTimeMillis()
        validatePackageAndDependencies(pkg,mode)
        println(s"validation finished in ${System.currentTimeMillis()-t0} ms" )
        Some(pkg)
      }
    }
  }

  def validatePackageAndDependencies(pkg: LinkablePackage, mode: ProcessingMode): Unit ={
    val pkgNames = mutable.Set.empty[String]

    processDependencies(pkg, mode){ pkg =>
      if(pkgNames.contains(pkg.name)){
        feedback(PlainMessage("The name "+pkg.name+" is used by more than one Package", Error, Checking()))
      } else {
        pkgNames.add(pkg.name)
      }
    }

    val tasks = pkg.dependencies.map(IndependentJob[LinkablePackage,Unit](validatePackageAndDependencies(_,mode)))
    val allTasks = tasks :+ IndependentJob{ () =>
      val t0 = System.currentTimeMillis()
      PackageValidator.validatePackage(pkg)
      println(s"validation of package ${pkg.name} finished in ${System.currentTimeMillis()-t0} ms" )
    }

    JobExecutor.executeIndependentJobs(JobResultBuilder(),allTasks)
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

}
