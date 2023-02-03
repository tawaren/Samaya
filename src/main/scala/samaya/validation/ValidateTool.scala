package samaya.validation

import samaya.plugin.service._
import samaya.structure.LinkablePackage
import samaya.types._


object ValidateTool {

  def main(args: Array[String]): Unit = validate(args(0))
  def validate(target:String):Option[LinkablePackage] = {
    //todo: have a multi location selector
    val parent:Directory = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = AddressResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    AddressResolver.resolve(parent,ident, PackageEncoder.Loader, Some(Set(PackageEncoder.packageExtensionPrefix))) match {
      case None => throw new Exception("Package not found: "+parent+"/"+ident)
      case Some(pkg) =>
        validatePackageAndDependencies(pkg)
        Some(pkg)
    }
  }

  def validatePackageAndDependencies(pkg: LinkablePackage): Unit ={
    //Todo: Have a cache so we do not valudate any times if imported more than onvce
    pkg.dependencies.foreach(validatePackageAndDependencies)
    PackageValidator.validatePackage(pkg)
  }
}
