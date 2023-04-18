package samaya

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{PlainMessage, feedback}
import samaya.plugin.PluginManager
import samaya.plugin.service.TaskExecutor

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
