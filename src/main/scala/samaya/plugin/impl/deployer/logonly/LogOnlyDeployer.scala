package samaya.plugin.impl.deployer.logonly

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Info, PlainMessage, feedback}
import samaya.deploy.Deployer
import samaya.plugin.service.Selectors
import samaya.structure.{Interface, Module, Transaction}
import samaya.structure.types.Hash

class LogOnlyDeployer extends Deployer{

  override def matches(s: Selectors.DeployerSelector): Boolean = true

  override def deployModule(module:Interface[_] with Module): Option[Hash] = {
    module.mode match {
      case Module.Precompile(id) => feedback(PlainMessage(s"Received precompiled module ${module.name} with id ${id} and hash ${module.meta.codeHash.getOrElse("null")} for deployment - deployment ignored", Info, ErrorManager.Deployer()))
      case Module.Elevated => feedback(PlainMessage(s"Received system module ${module.name} with hash ${module.meta.codeHash.getOrElse("null")} for deployment - deployment ignored", Info, ErrorManager.Deployer()))
      case Module.Normal => feedback(PlainMessage(s"Received module ${module.name} with hash ${module.meta.codeHash.getOrElse("null")} for deployment - deployment ignored", Info, ErrorManager.Deployer()))
    }
    module.meta.codeHash
  }

  override def deployTransaction(txt:Interface[_] with Transaction): Option[Hash] = {
    feedback(PlainMessage(s"Received transaction code ${txt.name} with hash ${txt.meta.codeHash.getOrElse("null")} for deployment - deployment ignored", Info, ErrorManager.Deployer()))
    txt.meta.codeHash
  }
}