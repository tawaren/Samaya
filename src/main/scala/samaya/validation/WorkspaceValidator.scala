package samaya.validation

import samaya.compilation.ErrorManager.{Error, PlainMessage, feedback}
import samaya.types.Workspace

object WorkspaceValidator {
  //validate the package integrety
  def validateWorkspace(wsp:Workspace): Unit = {
    if(wsp.name.length == 0 || wsp.name.charAt(0).isUpper) {
      feedback(PlainMessage("Workspace names must start with a lowercase Character", Error))
    }

    wsp.includes match {
      case Some(incls) => for(i <- incls) validateWorkspace(i)
      case None =>
    }

    wsp.dependencies match {
      case Some(deps) => for(d <- deps) PackageValidator.validatePackage(d)
      case None =>
    }
  }
}
