package samaya.deploy

import samaya.ProjectUtils.{buildIfMissing, processDependencies}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{PlainMessage, feedback, unexpected}
import samaya.deploy.DeployTask.{TaskName, mode, target}
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue}
import samaya.plugin.service.{Selectors, TaskExecutor}
import samaya.structure.types.Hash
import samaya.structure.{Component, Interface, LinkablePackage}
import samaya.types.{Deep, Fresh, ProcessingMode, Shallow}

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
    buildIfMissing(target, "Skipped deployment due to compilation error") { lp =>
      deployPackage(lp, mutable.HashSet.empty, mode)
    }
  }

  private def deployPackage(lp:LinkablePackage, deployed:mutable.Set[Hash], mode:ProcessingMode):Unit = {
    val t0 = System.currentTimeMillis()
    //deploy all dependencies
    processDependencies(lp,mode)(deployPackage(_,deployed, mode))
    //deploy all modules
    //Note: Build tool linearizes Module so that dependencies are full filled
    //      Meaning we deploy in the same order as we compile and thus we ensure dependencies are available
    lp.components.foreach(deploy(_,deployed))
    println(s"deployment of package ${lp.name} finished in ${System.currentTimeMillis()-t0} ms" )

  }

  private def deploy(mc:Interface[Component], deployed:mutable.Set[Hash]):Unit = {
    mc.meta.codeHash match {
      case Some(hash) =>
        if(deployed.add(hash)){
          mc.meta.code match {
            case None => unexpected(s"Can not deploy ${mc.name} as code is missing", ErrorManager.Deployer())
            case Some(_) => Deployer.deploy(mc) match {
              case None => feedback(PlainMessage(s"Deployment of ${mc.name} failed", ErrorManager.Error, ErrorManager.Deployer()))
              case Some(deployHash) => if(hash != deployHash) {
                feedback(PlainMessage(s"Deployment of ${mc.name} failed", ErrorManager.Error, ErrorManager.Deployer()))
              }
            }
          }
        }
      case None => //Nothing todo for things that can not be deployed
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



}