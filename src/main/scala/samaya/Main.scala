package samaya

import samaya.config.Config
import samaya.plugin.PluginManager
import samaya.plugin.service.TaskExecutor

object Main {
  def main(args: Array[String]): Unit = {
    Config.init(args)
    PluginManager.init()
    TaskExecutor.execute(Config.command.value)
  }
}
