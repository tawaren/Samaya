package samaya.build

import samaya.ProjectUtils
import samaya.build.CleanTask.{TaskName, target}
import samaya.compilation.ErrorManager.canProduceErrors
import samaya.plugin.config.{ConfigPluginCompanion, ConfigValue, RawPluginCompanion}
import samaya.plugin.service.{AddressResolver, Selectors, TaskExecutor}
import samaya.types.Workspace

import scala.reflect.ClassTag

class CleanTask extends TaskExecutor{

  override def matches(s: Selectors.TaskExecutorSelector): Boolean = s match {
    case Selectors.SelectByName(TaskName) => true
    case _ => false
  }


  override def execute(name: String): Unit = clean(target.value)
  override def apply[S: ClassTag, T: ClassTag](name: String, src: S): Option[T] = None

  //Todo: Abstract is nearly identical to compile
  def clean(target:String):Unit = {
    val t0 = System.currentTimeMillis()
    val wp = ProjectUtils.createWorkspace(target);
    cleanWorkspace(wp)
    println(s"cleaning of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
  }

  //Todo: if we do these todoes pack them into helper & share
  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff
  private def cleanWorkspace(wp:Workspace):Unit =  {
    //todo: look for a default location (probably the include folder in parent)
    //todo:  or search subdirectories for workspace files
    canProduceErrors{
      wp.includes.foreach(cleanWorkspace)
    }

    //Can we be more gentle??
    AddressResolver.deleteDirectory(wp.codeLocation)
    AddressResolver.deleteDirectory(wp.interfaceLocation)
    AddressResolver.deleteDirectory(wp.packageTarget)
  }
}

object CleanTask extends ConfigPluginCompanion {

  val TaskName: String = "clean"
  val target: ConfigValue[String] =  param(1).default("/")

}