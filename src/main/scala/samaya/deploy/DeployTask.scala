package samaya.deploy

import samaya.ProjectUtils.{buildIfMissing, processWitContextRepo, processWithArgRepos}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Compiler, PlainMessage, canProduceErrors, feedback, producesErrorValue, unexpected}
import samaya.deploy.DeployTask.{TaskName, mode, repos, target, validation}
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.impl.compiler.mandala.validate.MandalaValidator.opt
import samaya.plugin.service.{Selectors, TaskExecutor}
import samaya.structure.types.Hash
import samaya.structure.{Component, Interface, LinkablePackage}
import samaya.types.{Deep, Fresh, ProcessingMode, Repository, Shallow}
import samaya.validation.PackageValidator

import scala.collection.mutable
import scala.reflect.ClassTag

class DeployTask extends TaskExecutor{

  override def matches(s: Selectors.TaskExecutorSelector): Boolean = s match {
    case Selectors.SelectByName(TaskName) => true
    case _ => false
  }
  override def execute(name: String): Unit = deploy(target.value, mode.value)
  override def apply[S: ClassTag, T: ClassTag](name: String, src: S): Option[T] = None


  def deploy(target:String, mode:ProcessingMode = Fresh):Unit = {
    processWithArgRepos(repos.value){
      buildIfMissing(target, "Skipped deployment due to compilation or verification error") { lp =>
        if(validation.value){
          val hasError = canProduceErrors(PackageValidator.validatePackage(lp, recursive = true))
          if(hasError){
            feedback(PlainMessage(s"Package ${lp.name} is invalid", ErrorManager.Error, ErrorManager.Deployer()))
            return
          } else {
            feedback(PlainMessage(s"Validation of Package ${lp.name} succeeded", ErrorManager.Info, ErrorManager.Deployer()))
          }
        }
        deployPackage(lp, mutable.HashSet.empty, mode)
      }
    }
  }


  //We make sequential as depending on deployer parallel is not an option or would not give any benefits
  // We make a hash based deduplication so we deploy everything only once
  private def deployPackage(lp:LinkablePackage, deployed:mutable.Set[Hash], mode:ProcessingMode):Boolean = {
    if(deployed.add(lp.hash)){
      val t0 = System.currentTimeMillis()
      //Todo: Make helper
      val includes = mode match {
        case Deep => lp.dependencies.map(_.name).toSet
        case Fresh => lp.includes.getOrElse(Set.empty)
        case Shallow => Set.empty[String]
      }
      processWitContextRepo(lp) {
        //deploy all dependencies
        val deps = lp.dependencies.filter(p => includes.contains(p.name))
        for(dep <- deps) {
          if(!deployPackage(dep, deployed, mode)) {
            feedback(PlainMessage(s"Deployment of Package ${dep.name} failed", ErrorManager.Error, ErrorManager.Deployer()))
            return false
          }
        }
        deployed.add(lp.hash)
        for(cp <- lp.components ) {
          if(!deploy(cp,deployed)) {
            feedback(PlainMessage(s"Deployment of Component ${cp.name} failed", ErrorManager.Error, ErrorManager.Deployer()))
            return false
          }
        }
      }
      println(s"deployment of package ${lp.name} finished in ${System.currentTimeMillis()-t0} ms" )
    }
    true
  }

  private def deploy(mc:Interface[Component], deployed:mutable.Set[Hash]):Boolean = {

    mc.meta.codeHash match {
      case Some(hash) =>
        if(deployed.add(hash)){
          mc.meta.code match {
            case None => unexpected(s"Can not deploy ${mc.name} as code is missing", ErrorManager.Deployer())
            case Some(_) => Deployer.deploy(mc) match {
              case None =>
                feedback(PlainMessage(s"Deployment of ${mc.name} failed", ErrorManager.Error, ErrorManager.Deployer()))
                return false
              case Some(deployHash) => if(hash != deployHash) {
                feedback(PlainMessage(s"Deployment of ${mc.name} failed", ErrorManager.Error, ErrorManager.Deployer()))
                return false
              }
            }
          }
        }
        true
      case None if mc.isVirtual=> true
      case None =>
        feedback(PlainMessage(s"Code hash for ${mc.name} is missing", ErrorManager.Error, ErrorManager.Deployer()))
        false
    }
  }
}

//Todo: Switch to a CommandLine Parsing library
object DeployTask extends ConfigPluginCompanion {

  val TaskName: String = "deploy"

  val target: ConfigValue[String] =  param(1).default("/")
  val mode: ConfigValue[ProcessingMode] = select(
    "r|d|recursive|deep" -> Deep,
    "t|s|top|shallow" -> Shallow
  ).default(Fresh)

  private val validation : ConfigValue[Boolean] = opt("deploy.validation|validation").default(true)
    .warnIfFalse("Validation of Mandala modules and instances is disabled",ErrorManager.Deployer())

  private val repos : ConfigValue[Seq[String]] = collectArg("deploy.repos|deploy.repositories|repositories|repos").default(Seq.empty)

}
