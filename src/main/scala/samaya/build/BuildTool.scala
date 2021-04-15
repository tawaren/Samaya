package samaya.build


import samaya.build.desc.Dependency
import samaya.compilation.ErrorManager
import samaya.plugin.service.{LanguageCompiler, LocationResolver, PackageEncoder, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types.{Identifier, InputSource, Location, Path, Workspace}
import samaya.validation.WorkspaceValidator
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, PlainMessage, Warning, canProduceErrors, feedback, unexpected}
import samaya.plugin.impl.pkg.json.JsonModel.Info
import samaya.plugin.impl.wp.json.WorkspaceImpl


object BuildTool {

  def main(args: Array[String]): Unit = build(args(0))
  def build(target:String):Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()

    //todo: have a multi location selector
    val parent:Location = LocationResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = LocationResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    val wp = LocationResolver.resolveSource(parent,ident, Some(Set(WorkspaceEncoder.workspaceExtensionPrefix))) match {
      case None => LocationResolver.resolveLocation(parent, ident) match {
        case None => throw new Exception("Workspace not found"); //todo: error
        case Some(workFolder) => implicitWorkspace(workFolder)
      }
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

  def implicitWorkspace(workFolder:Location): Workspace = {
      var includes = Set.empty[Workspace]
      var wspList = Set.empty[String]
      var preDeps = Map.empty[String,InputSource]
      var explicits = Set.empty[Location]
      var comps = Set.empty[Path]
      val sources = LocationResolver.listSources(workFolder)

      for(source <- sources) {
        source.extension match {
          case None =>
            //Todo: add deps files as dependencies unless their is a workspaceFile as well
          case Some(ext) if ext.startsWith(WorkspaceEncoder.workspaceExtensionPrefix) =>
            val input = LocationResolver.resolveSource(workFolder, Path(source)) match {
              case None => throw new Exception("Workspace could not be loaded");//todo: error (inclusive position)
              case Some(input) => input
            }
            WorkspaceEncoder.deserializeWorkspace(input) match {
              case Some(value) =>
                WorkspaceValidator.validateWorkspace(value)
                explicits = explicits + value.workspaceLocation
                wspList = wspList + source.name
                includes = includes + value;
              case None => throw new Exception("Workspace could not be loaded");//todo: error
            }
          case Some(ext) if ext.startsWith(PackageEncoder.packageExtensionPrefix) =>
              val input = LocationResolver.resolveSource(workFolder, Path(source)) match {
                case None => throw new Exception("Workspace could not be loaded");//todo: error (inclusive position)
                case Some(input) => input
              }
              preDeps = preDeps.updated(source.name, input)
          //Todo: Ask compilers for valid source extensions & check it is one
          case Some(_) => comps = comps + Path(source)
        }
      }
      val folders = LocationResolver.listLocations(workFolder);
      for(folder <- folders) {
        if(folder.name != "out" && folder.name != "abi") {
          LocationResolver.resolveLocation(workFolder, Path(folder)) match {
            case Some(newWorkFolder) if !explicits.contains(newWorkFolder) =>
              includes = includes + implicitWorkspace(newWorkFolder)
            case _ =>
          }
        }
      }

      val dependencies = preDeps
        .filter(kv => !wspList.contains(kv._1))
        .values
        .filter(pkg => pkg.location != workFolder && pkg.identifier.name == workFolder.name)
        .flatMap(PackageEncoder.deserializePackage)
        .toSet

      //todo: allow to specify a root dir and build a parallel tree
      val out =  LocationResolver.resolveLocation(workFolder,Path(Identifier("out")), create = true)
      val inter = LocationResolver.resolveLocation(workFolder,Path(Identifier("abi")), create = true)

      new WorkspaceImpl(
        workFolder.name,
        workFolder,
        Some(includes),
        Some(dependencies),
        Some(comps),
        workFolder,
        out.get,
        inter.get
      )
  }

  private case class Job(source:InputSource, dependencies:Set[Dependency])

  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff
  private def compileWorkspace(wp:Workspace):Option[LinkablePackage] = {
    var depsBuilder = Map.newBuilder[String,LinkablePackage]
    //todo: look for a default location (probably the include folder in parent)
    //todo:  or search subdirectories for workspace files
    val includes =  wp.includes.getOrElse(Set.empty)
    //todo: look for a default location (probably the dependency folder in parent)
    //todo:  or search subdirectories for interface files
    val dependencies =  wp.dependencies.getOrElse(Set.empty)

    depsBuilder.sizeHint(includes.size + dependencies.size)
    val depsError = canProduceErrors{
      includes.foreach(iwp => {
        compileWorkspace(iwp) match {
          case Some(pkg) =>
            depsBuilder += pkg.name -> pkg
            //Serialize
            PackageEncoder.serializePackage(pkg, iwp)
          case None =>
        }
      })
    }

    if(!depsError) {
      dependencies.foreach(pkg => depsBuilder += pkg.name -> pkg)
      val resolvedDeps = depsBuilder.result()

      if(resolvedDeps.size != includes.size + dependencies.size){
        throw new Exception("NEEDS A CUSTOM ERROR: Alla ambigous dependencies")
      }

      val t0 = System.currentTimeMillis()

      val source = wp.sourceLocation
      val code = wp.codeLocation
      val interface = wp.interfaceLocation

      val compSources = wp.sources match {
        case Some(srcs) => srcs.flatMap { src =>
          LocationResolver.resolveSource(source, src) match {
            case None =>
              feedback(PlainMessage(s"Could not find $src", Error, Builder()))
              None
            case Some(moduleSource) => Some(moduleSource)
          }
        }
        case None =>  LocationResolver.listSources(source).flatMap{ src =>
          LocationResolver.resolveSource(source, Path(src))
        }
      }

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
      Some(resPkg)
    } else {
      feedback(PlainMessage(s"Did not compile ${wp.name} in ${wp.workspaceLocation} because of errors in dependencies", ErrorManager.Info, Builder()))
      None
    }
  }

  private class OrderedCompilation(
              name:String,
              code:Location,
              interface:Location,
              resolvedDeps:Map[String, LinkablePackage],
              nameFileMapping:Map[String,String],
              var uncompiled:Map[String, Job],
  ) {

    private var compilationFailed = Set[String]()

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
      val depsError = canProduceErrors{
        for (Dependency(path, sources) <- nextValue.dependencies) {
          //it is in the same package
          if (path.head.length > 0 && path.head.charAt(0).isUpper) {
            nameFileMapping.get(path.head) match {
              case None => feedback(LocatedMessage(s"Component ${path.mkString(".")} is missing in workspace", sources ,Error,Builder()))
              case Some(targ) =>
                if(uncompiled.contains(targ)) {
                  compile(targ)
                } else if(!compiled.contains(targ) && compilationFailed.contains(targ)){
                  feedback(LocatedMessage(s"Component ${path.mkString(".")} was not declared in dependencies", sources ,Error,Builder()))
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
                    feedback(LocatedMessage(s"Workspace does not contain dependency to ${path.mkString(".")}", sources ,Error, Builder()))
                  }
                }
              case None => feedback(LocatedMessage(s"Workspace or Package $pkgName is missing", sources ,Error, Builder()))
            }
          }
        }
      }
      if(!depsError) {
        partialPkg = ComponentBuilder.build(nextValue.source, code, interface, partialPkg)
        compiled = compiled + nextKey
      } else {
        feedback(PlainMessage(s"Did not compile ${nextValue.source.identifier} in ${nextValue.source.location} because of errors in dependencies", ErrorManager.Info, Builder()))
        compilationFailed = compilationFailed + nextKey
      }
    }
  }
}
