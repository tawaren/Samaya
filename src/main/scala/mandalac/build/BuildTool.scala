package mandalac.build

import mandalac.plugin.service.{LanguageCompiler, LocationResolver, PackageManager, WorkspaceManager}
import mandalac.structure.Module
import mandalac.types
import mandalac.types.{Identifier, InputSource, Location, Workspace}

object BuildTool {
  def main(args: Array[String]): Unit = {
    //todo: have a multi location selector
    val parent:Location = LocationResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = LocationResolver.parsePath(args(0)) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    val wp = LocationResolver.resolveSource(parent,ident) match {
      case None => throw new Exception("Workspace file not found");//todo: error
      case Some(input) => WorkspaceManager.deserializeWorkspace(input) match {
        case Some(value) => value
        case None => throw new Exception("Workspace could not be loaded");//todo: error
      }
    }

    //todo: shall we have root path from some where else, to allow compiling sub workspaces individually???
    compileWorkspace(wp:Workspace, List.empty)
  }

  private case class Job(source:InputSource, dependencies:Set[Seq[String]])

  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff
  private def compileWorkspace(wp:Workspace, path:Seq[Identifier]):types.Package = {
    var depsBuilder = Map.newBuilder[String,types.Package]
    //todo: look for a default location (probably the include folder in parent)
    //todo:  or search subdirectories for workspace files
    val includes =  wp.includes.getOrElse(Set.empty)
    //todo: look for a default location (probably the dependency folder in parent)
    //todo:  or search subdirectories for interface files
    val dependencies =  wp.dependencies.getOrElse(Set.empty)
    val subPath = path :+ Identifier(wp.name)
    depsBuilder.sizeHint(includes.size + dependencies.size)
    includes.foreach(iwp => {
      val pkg = compileWorkspace(iwp,subPath)
      depsBuilder += pkg.name -> pkg
      //Serialize
      PackageManager.serializePackage(wp.workspaceLocation, pkg, iwp)

    })
    dependencies.foreach(pkg => depsBuilder += pkg.name -> pkg)
    val resolvedDeps = depsBuilder.result()
    if(resolvedDeps.size != includes.size + dependencies.size){
      throw new Exception("NEEDS A CUSTOM ERROR: Alla ambigous dependencies")
    }

    val source = wp.sourceLocation
    //todo: look for defaults (searching the source folder)
    val modules = wp.modules.getOrElse(Set.empty)
    var uncompiled = modules.map(module =>
      LocationResolver.resolveSource(source,module) match {
        case None => throw new Exception("NEEDS A CUSTOM ERROR")
        case Some(moduleSource) => (moduleSource.identifier.name,Job(moduleSource, LanguageCompiler.extractDependencies(moduleSource)))
      }
    ).toMap

    var compiled = Map.empty[String, Seq[Module]]
    var partialPkg = PartialPackage(
      path = path,
      location = wp.workspaceLocation,
      name = wp.name,
      dependencies = resolvedDeps.values.toSeq
    )
    while(uncompiled.nonEmpty) { compile(uncompiled.keys.head) }

    def compile(nextKey:String): Unit = {
      val nextValue = uncompiled(nextKey)
      uncompiled = uncompiled - nextKey
      for(d <- nextValue.dependencies){
        //it is in the same package
        if(d.size == 1) {
          if(uncompiled.contains(d.head)){
            compile(d.head)
          } else {
            //todo: shall we allow packages wildcard dependencies
            if(!compiled.contains(d.head)) {
              throw new Exception("NEEDS A CUSTOM ERROR: ALLA DEPENDENCY MISSING")
            }
          }
        } else {
          if(resolvedDeps.get(d.head).exists(pkg => pkg.allModulesByPath(d.tail).nonEmpty)){
            throw new Exception("NEEDS A CUSTOM ERROR: ALLA DEPENDENCY MISSING")
          }
        }
      }
      //todo: Error Scope
      //todo: Error if not found
      val res = LanguageCompiler.compileFully(nextValue.source, partialPkg)
      if(res.isEmpty) {
        throw new Exception("No Compiler output could be produced for module: "+nextKey)
      }

      //todo: lift the ModuleEssentials to a Module
      //      1: Produce Code
      //      2: Produce Interface (withCodeHash)
      //      3: Set Hash + Meta


      res.foreach { mod =>
        partialPkg = partialPkg.withModule(mod)
      }
      compiled = compiled.updated(nextKey, res)

      println(partialPkg)

      //todo: Calc & Set Package Hash
      partialPkg.withHash(???)
    }
  }

  private def compilePackage():Unit = {

  }

}
