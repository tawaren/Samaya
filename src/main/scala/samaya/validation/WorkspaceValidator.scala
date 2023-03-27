package samaya.validation

import samaya.compilation.ErrorManager.{Checking, Error, PlainMessage, Warning, feedback}
import samaya.plugin.service.AddressResolver
import samaya.types.{Identifier, Workspace}

import scala.collection.mutable

object WorkspaceValidator {
  //validate the package integrity
  def validateWorkspace(wsp:Workspace, recursive:Boolean = false): Unit = {
    if(wsp.name.isEmpty || wsp.name.charAt(0).isUpper) {
      feedback(PlainMessage(s"Workspace ${wsp.location} names must start with a lowercase Character", Error, Checking()))
    }

    val names = mutable.Set.empty[String]
    for(i <- wsp.includes) {
      if(names.contains(i.name)){
        feedback(PlainMessage("The name "+i.name+" is used by more than one Package or Workspace", Error, Checking()))
      } else {
        names.add(i.name)
      }
      if(recursive)validateWorkspace(i)
    }
    for(d <- wsp.dependencies) {
      if(names.contains(d.name)){
        feedback(PlainMessage("The name "+d.name+" is used by more than one Package or Workspace", Error, Checking()))
      } else {
        names.add(d.name)
      }
      if(recursive)PackageValidator.validatePackage(d, true)
    }

    val srcNames = mutable.Set.empty[Identifier]
    for(s <- wsp.sources) {
      AddressResolver.resolve(wsp.sourceLocation.resolveAddress(s), AddressResolver.SourceLoader) match {
        case Some(src) => if(srcNames.contains(src.identifier)){
          feedback(PlainMessage("The source name "+src.identifier.fullName+" is used by more than one source file", Warning, Checking()))
        } else {
          srcNames.add(src.identifier)
        }
        case None => feedback(PlainMessage("The source "+wsp.sourceLocation.resolveAddress(s)+" does not exist", Error, Checking()))
      }

    }

  }
}
