package samaya.plugin.service.category

import samaya.deploy.Deployer
import samaya.plugin.service.PluginCategory

object DeployerPluginCategory extends PluginCategory[Deployer]{
  override def name: String = "deployer"
  override def interface: Class[Deployer] = classOf[Deployer]
}
