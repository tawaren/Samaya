package samaya.deploy

import samaya.build.BuildTool
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager._
import samaya.structure.types.Hash
import samaya.structure.{Component, LinkablePackage, Interface, ModuleInterface, TransactionInterface, Transaction}

import scala.collection.mutable

object DeployTool {
  def main(args: Array[String]): Unit = deploy(args(0))
  def deploy(target:String):Unit = {
    ErrorManager.producesErrorValue(BuildTool.build(target)) match {
      case None => feedback(PlainMessage("Skipped Deployment due to compilation error", Info))
      case Some(lp) => deployPackage(lp, mutable.HashSet())
    }
  }

  private def deployPackage(lp:LinkablePackage, deployed:mutable.Set[Hash]):Unit = {
    val t0 = System.currentTimeMillis()
    //deploy all dependencies
    lp.dependencies.filter(!_.interfacesOnly).foreach(deployPackage(_,deployed))
    //deploy all modules
    //Note: Build tool linearizes Module so that dependencies are full filled
    //      Meaning we deploy in the same order as we compile and thus we ensure dependencies are avaiable
    lp.components.foreach(deploy(_,deployed))
    println(s"deployment of package ${lp.name} finished in ${System.currentTimeMillis()-t0} ms" )

  }

  private def deploy(mc:Interface[Component], deployed:mutable.Set[Hash]):Unit = {
    mc.meta.codeHash match {
      case Some(hash) =>
        if(deployed.add(hash)){
          mc.meta.code match {
            case None => unexpected(s"Can not deploy ${mc.name} as code is missing")
            case Some(code) => Deployer.deploy(mc) match {
              case None => unexpected(s"Deployment of ${mc.name} failed")
              case Some(deployHash) =>
                if(hash != deployHash) unexpected(s"Target Platform produced a different hash then compiler $deployHash vs. $hash")
            }
          }
        }
      case None => //Nothing todo for things that can not be deployed
    }
  }
}
