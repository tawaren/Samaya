package samaya.deploy

import samaya.plugin.service.category.DeployerPluginCategory
import samaya.plugin.service.{PluginCategory, Selectors}
import samaya.plugin.{Plugin, PluginProxy}
import samaya.structure.{Component, Interface, Module, Transaction}
import samaya.structure.types.Hash

import scala.reflect.ClassTag

trait Deployer extends Plugin{
  override type Selector = Selectors.DeployerSelector

  //Todo: Move to the bottom
  def deploy(comp:Interface[Component]):Option[Hash] = {
    comp match {
      case module: Interface[_] with Module => deployModule(module)
      case transaction: Interface[_] with Transaction => deployTransaction(transaction)
    }
  }
  //both return true if deployment suceeded
  def deployModule(module:Interface[_] with Module):Option[Hash]
  def deployTransaction(txt:Interface[_] with Transaction):Option[Hash]
}

object Deployer extends Deployer with PluginProxy{

  type PluginType = Deployer
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]

  override def category: PluginCategory[PluginType] = DeployerPluginCategory

  override def deployModule(module:Interface[_] with Module): Option[Hash]  = {
    select(Selectors.ModuleDeployerSelector).flatMap(r => r.deployModule(module))
  }

  override def deployTransaction(txt:Interface[_] with Transaction): Option[Hash]  = {
    select(Selectors.TransactionDeployerSelector).flatMap(r => r.deployTransaction(txt))
  }


}
