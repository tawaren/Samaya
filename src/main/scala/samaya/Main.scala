package samaya

import samaya.build.{BuildTask, CleanTask}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{PlainMessage, feedback}
import samaya.deploy.DeployTask
import samaya.pakage.PackageTask
import samaya.plugin.PluginManager
import samaya.plugin.config.ParameterAndOptions
import samaya.plugin.service.TaskExecutor
import samaya.validation.ValidateTask

object Main {
  def main(args: Array[String]): Unit = {
    if(args.length == 0) {
        println("to few parameters")
    } else{
      val cmd = PluginManager.init(args)
      val command = cmd.parameters.headOption;

      command match {
        case Some(task) => TaskExecutor.execute(task)
        case None => feedback(PlainMessage(s"No task was specified", ErrorManager.Error, ErrorManager.Always))
      }
    }
  }
}
