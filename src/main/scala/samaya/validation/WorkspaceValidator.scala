package samaya.validation

import samaya.compilation.ErrorManager.{Checking, Error, PlainMessage, feedback}
import samaya.types.Workspace

object WorkspaceValidator {
  //validate the package integrety
  def validateWorkspace(wsp:Workspace): Unit = {
    if(wsp.name.length == 0 || wsp.name.charAt(0).isUpper) {
      feedback(PlainMessage(s"Workspace ${wsp.workspaceLocation} names must start with a lowercase Character", Error, Checking()))
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
