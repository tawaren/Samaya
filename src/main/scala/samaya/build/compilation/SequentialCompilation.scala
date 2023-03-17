package samaya.build.compilation

import samaya.build.compilation.Compilation.Job
import samaya.build.compilation.pkgbuilder.ImmutablePackageBuilder
import samaya.build.{ComponentBuilder, PartialPackage}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, PlainMessage, canProduceErrors, feedback}
import samaya.structure.LinkablePackage
import samaya.types.Directory

class SequentialCompilation(
                          name:String,
                          code:Directory,
                          interface:Directory,
                          resolvedDeps:Map[String, LinkablePackage],
                          nameFileMapping:Map[String,String],
                          var uncompiled:Map[String, Job],
                        ) extends Compilation{

  private var compilationFailed = Set[String]()

  private var partialPkg: ImmutablePackageBuilder = ImmutablePackageBuilder(
    name = name,
    dependencies = resolvedDeps.values.toSeq
  )

  def compileAll(): PartialPackage[_] = {
    while (uncompiled.nonEmpty) {
      compile(uncompiled.keys.head)
    }
    partialPkg
  }

  private def compile(nextKey: String): Unit = {
    val nextValue = uncompiled(nextKey)
    uncompiled = uncompiled - nextKey
    val depsError = canProduceErrors{
      for (Dependency(path, sources) <- nextValue.dependencies) {
        //it is in the same package
        if (path.head.nonEmpty && path.head.charAt(0).isUpper) {
          nameFileMapping.get(path.head) match {
            case None => feedback(LocatedMessage(s"Component ${path.mkString(".")} is missing in workspace", sources ,Error,Builder()))
            case Some(targ) =>
              if(uncompiled.contains(targ)) {
                compile(targ)
              } else if(compilationFailed.contains(targ)){
                feedback(LocatedMessage(s"Component ${path.mkString(".")} was not declared in dependencies", sources ,Error,Builder()))
              }
          }
        } else {
          val pkgName = path.head
          resolvedDeps.get(pkgName) match {
            case Some(pkg) =>
              val compPath = path.tail.takeWhile(ep => ep.nonEmpty && ep.charAt(0).isLower)
              val compName = path.tail.drop(compPath.length)
              if(compName.nonEmpty) {
                if (pkg.componentByPathAndName(compPath, compName.head).isEmpty) {
                  feedback(LocatedMessage(s"Workspace does not contain dependency to ${path.mkString(".")}", sources ,Error, Builder()))
                }
              }
            case None => feedback(LocatedMessage(s"Workspace or Package $pkgName is missing", sources ,Error, Builder()))
          }
        }
      }
    }
    if(!depsError) {
      partialPkg = ComponentBuilder.build(nextValue.source, code, interface, partialPkg).asInstanceOf[ImmutablePackageBuilder]
    } else {
      feedback(PlainMessage(s"Did not compile ${nextValue.source.identifier} in ${nextValue.source.location} because of errors in dependencies", ErrorManager.Info, Builder()))
      compilationFailed = compilationFailed + nextKey
    }
  }
}