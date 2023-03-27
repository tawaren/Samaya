package samaya.plugin.service.category

import samaya.plugin.service.{JobExecutor, PluginCategory}

object JobExecutorPluginCategory extends PluginCategory[JobExecutor]{
  override def name: String = "job_executor"
  override def interface: Class[JobExecutor] = classOf[JobExecutor]
}
