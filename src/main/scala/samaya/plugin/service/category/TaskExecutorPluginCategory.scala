package samaya.plugin.service.category

import samaya.plugin.service.{PluginCategory, TaskExecutor}

object TaskExecutorPluginCategory extends PluginCategory[TaskExecutor]{
  override def name: String = "task_executor"
  override def interface: Class[TaskExecutor] = classOf[TaskExecutor]
}
