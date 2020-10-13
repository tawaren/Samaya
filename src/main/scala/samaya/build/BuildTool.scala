package samaya.build

import java.security.Security

import com.rfksystems.blake2b.security.Blake2bProvider
import samaya.build.desc.Dependency
import samaya.plugin.service.{LanguageCompiler, LocationResolver, PackageEncoder, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types.{InputSource, Location, Workspace}
import samaya.validation.WorkspaceValidator
import samaya.compilation.ErrorManager.{Error, LocatedMessage, PlainMessage, Warning, feedback, unexpected}


object BuildTool {

  def main(args: Array[String]): Unit = build(args(0))
  def build(target:String):LinkablePackage = {
    val t0 = System.currentTimeMillis()
    //We need blake at different places - if we have more of these deps, we should have a setup object
    Security.addProvider(new Blake2bProvider)

    //todo: have a multi location selector
    val parent:Location = LocationResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = LocationResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    val wp = LocationResolver.resolveSource(parent,ident, Some(Set(WorkspaceEncoder.workspaceExtensionPrefix))) match {
      case None => throw new Exception("Workspace file not found");//todo: error
      case Some(input) => WorkspaceEncoder.deserializeWorkspace(input) match {
        case Some(value) =>
          WorkspaceValidator.validateWorkspace(value)
          value
        case None => throw new Exception("Workspace could not be loaded");//todo: error
      }
    }
    val res = compileWorkspace(wp)
    println(s"compilation of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
    res
  }

  private case class Job(source:InputSource, dependencies:Set[Dependency])

  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff
  private def compileWorkspace(wp:Workspace):LinkablePackage = {
    var depsBuilder = Map.newBuilder[String,LinkablePackage]
    //todo: look for a default location (probably the include folder in parent)
    //todo:  or search subdirectories for workspace files
    val includes =  wp.includes.getOrElse(Set.empty)
    //todo: look for a default location (probably the dependency folder in parent)
    //todo:  or search subdirectories for interface files
    val dependencies =  wp.dependencies.getOrElse(Set.empty)

    depsBuilder.sizeHint(includes.size + dependencies.size)
    includes.foreach(iwp => {
      val pkg = compileWorkspace(iwp)
      depsBuilder += pkg.name -> pkg
      //Serialize
      PackageEncoder.serializePackage(pkg, iwp)

    })
    dependencies.foreach(pkg => depsBuilder += pkg.name -> pkg)
    val resolvedDeps = depsBuilder.result()

    if(resolvedDeps.size != includes.size + dependencies.size){
      throw new Exception("NEEDS A CUSTOM ERROR: Alla ambigous dependencies")
    }

    val t0 = System.currentTimeMillis()

    val source = wp.sourceLocation
    val code = wp.codeLocation
    val interface = wp.interfaceLocation

    val components = wp.components.getOrElse(Set.empty)
    val compSources = components.flatMap(comp =>
      LocationResolver.resolveSource(source,comp) match {
        case None =>
          feedback(PlainMessage(s"Could not find $comp", Error))
          None
        case Some(moduleSource) => Some(moduleSource)
      }
    )

    val uncompiled = compSources.map(compSource =>
      (compSource.identifier.name,Job(compSource, LanguageCompiler.extractDependencies(compSource)))
    ).toMap

    val nameFileMapping = compSources.flatMap(moduleSource =>
      LanguageCompiler.extractComponentNames(moduleSource).map(dep => (dep, moduleSource.identifier.name))
    ).toMap

    val state = new OrderedCompilation(wp.name, code, interface, resolvedDeps, nameFileMapping, uncompiled)

    val resPkg = state.compileAll().toLinkablePackage(wp.workspaceLocation)
    PackageEncoder.serializePackage(resPkg, wp)
    println(s"compilation of package ${resPkg.name} finished in ${System.currentTimeMillis()-t0} ms" )
    resPkg
  }

  private class OrderedCompilation(
              name:String,
              code:Location,
              interface:Location,
              resolvedDeps:Map[String, LinkablePackage],
              nameFileMapping:Map[String,String],
              var uncompiled:Map[String, Job],
  ) {

    private var partialPkg: PartialPackage = PartialPackage(
      name = name,
      dependencies = resolvedDeps.values.toSeq
    )

    private var compiled = Set[String]();

    def compileAll(): PartialPackage = {
      while (uncompiled.nonEmpty) {
        compile(uncompiled.keys.head)
      }
      partialPkg
    }

    private def compile(nextKey: String): Unit = {
      val nextValue = uncompiled(nextKey)
      uncompiled = uncompiled - nextKey
      for (Dependency(path, sources) <- nextValue.dependencies) {
        //it is in the same package
        if (path.head.length > 0 && path.head.charAt(0).isUpper) {
          nameFileMapping.get(path.head) match {
            case None => feedback(LocatedMessage(s"Component ${path.mkString(".")} is missing in workspace", sources ,Error))
            case Some(targ) =>
              if (uncompiled.contains(targ)) {
                compile(targ)
              } else if(!compiled.contains(targ)){
                feedback(LocatedMessage(s"Component ${path.mkString(".")} was not declared in dependencies", sources ,Error))
              }
          }
        } else {
          val pkgName = path.head
          resolvedDeps.get(pkgName) match {
            case Some(pkg) =>
              val compPath = path.tail.takeWhile(ep => ep.length > 0 && ep.charAt(0).isLower)
              val compName = path.tail.drop(compPath.length)
              if(compName.nonEmpty) {
                if (pkg.componentByPathAndName(compPath, compName.head).isEmpty) {
                  feedback(LocatedMessage(s"Workspace does not contain dependency to ${path.mkString(".")}", sources ,Warning))
                }
              }
            case None => feedback(LocatedMessage(s"Workspace or Package $pkgName is missing", sources ,Warning))
          }


        }
      }
      partialPkg = ComponentBuilder.build(nextValue.source, code, interface, partialPkg)
      compiled = compiled + nextKey
    }
  }
}
