package samaya.build


import samaya.build.desc.Dependency
import samaya.compilation.ErrorManager
import samaya.plugin.service.{ContentLocationIndexer, DependenciesEncoder, LanguageCompiler, AddressResolver, PackageEncoder, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types.{Identifier, InputSource, Directory, Address, Workspace}
import samaya.validation.WorkspaceValidator
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, PlainMessage, Warning, canProduceErrors, feedback, unexpected}
import samaya.plugin.impl.pkg.json.JsonModel.Info
import samaya.plugin.impl.wp.json.WorkspaceImpl


object BuildTool {

  def main(args: Array[String]): Unit = build(args(0))
  def build(target:String):Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()

    //todo: have a multi location selector
    val parent:Directory = AddressResolver.provideDefault().getOrElse(throw new Exception("A"))

    val ident = AddressResolver.parsePath(target) match {
      case Some(id) => id
      case None => throw new Exception("Illegal arg");//todo: error
    }

    val wp = AddressResolver.resolve(parent,ident, AddressResolver.InputLoader, Some(Set(WorkspaceEncoder.workspaceExtensionPrefix))) match {
      case None => AddressResolver.resolveDirectory(parent, ident) match {
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

  def implicitWorkspace(workFolder:Directory): Workspace = {
      var includes = Set.empty[Workspace]
      var wspList = Set(workFolder.name)
      var deps = Set.empty[LinkablePackage]
      var explicits = Set.empty[Directory]
      var comps = Set.empty[Address]
      val sources = AddressResolver.listSources(workFolder)

      val (wspSources, otherSources) = sources.partition{ source => source.extension match {
        case Some(ext) => ext.startsWith(WorkspaceEncoder.workspaceExtensionPrefix)
        case None => false
      }}

      for(source <- wspSources) {
        val input = AddressResolver.resolve(workFolder, Address(source), AddressResolver.InputLoader) match {
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
      }

      for(source <- otherSources) {
        source.extension match {
          case None =>
          case Some(ext) if ext.startsWith(DependenciesEncoder.dependenciesExtensionPrefix) =>
            val input = AddressResolver.resolve(workFolder, Address(source),AddressResolver.InputLoader) match {
              case None => throw new Exception("Dependencies could not be loaded");//todo: error (inclusive position)
              case Some(input) => input
            }

            DependenciesEncoder.deserializeDependenciesSources(input) match {
              case Some(dependencySources) => deps = deps ++ dependencySources
              case None => throw new Exception("Dependencies could not be loaded");//todo: error
            }
          case Some(ext) if ext.startsWith(PackageEncoder.packageExtensionPrefix) & !wspList.contains(source.name) =>
            //Todo: Make work then incomment the guard
            val pkg = AddressResolver.resolve(workFolder, Address(source), PackageEncoder.Loader) match {
              case None => throw new Exception("Package Dependency could not be loaded");//todo: error (inclusive position)
              case Some(input) => input
            }
            if(pkg.location != workFolder) {
              deps = deps + pkg
            }
          //Todo: Ask compilers for valid source extensions & check it is one
          //For now just ignore packages
          case Some(ext) if ext.startsWith(PackageEncoder.packageExtensionPrefix) =>
          case Some(_) => comps = comps + Address(source)
        }
      }
      val folders = AddressResolver.listDirectories(workFolder);
      for(folder <- folders) {
        //Todo: We should not hardcode?
        if(folder.name != "out" && folder.name != "abi") {
          AddressResolver.resolveDirectory(workFolder, Address(folder)) match {
            case Some(newWorkFolder) if !explicits.contains(newWorkFolder) =>
              includes = includes + implicitWorkspace(newWorkFolder)
            case _ =>
          }
        }
      }

      //todo: allow to specify a root dir and build a parallel tree
      val out =  AddressResolver.resolveDirectory(workFolder,Address(Identifier("out")), create = true)
      val inter = AddressResolver.resolveDirectory(workFolder,Address(Identifier("abi")), create = true)

      new WorkspaceImpl(
        workFolder.name,
        workFolder,
        Some(includes),
        Some(deps),
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
            updateContentIndexes(pkg)
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
          AddressResolver.resolve(source, src, AddressResolver.InputLoader) match {
            case None =>
              feedback(PlainMessage(s"Could not find $src", Error, Builder()))
              None
            case Some(moduleSource) => Some(moduleSource)
          }
        }
        case None =>  AddressResolver.listSources(source).flatMap{ src =>
          AddressResolver.resolve(source, Address(src), AddressResolver.InputLoader)
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
      updateContentIndexes(resPkg)
      println(s"compilation of package ${resPkg.name} finished in ${System.currentTimeMillis()-t0} ms" )
      Some(resPkg)
    } else {
      feedback(PlainMessage(s"Did not compile ${wp.name} in ${wp.workspaceLocation} because of errors in dependencies", ErrorManager.Info, Builder()))
      None
    }
  }

  def updateContentIndexes(pkg:LinkablePackage): Unit = {
    pkg.components.foreach(cmp => {
      ContentLocationIndexer.indexContent(Some(pkg.location), cmp.meta.interface)
      cmp.meta.sourceCode.foreach(s => ContentLocationIndexer.indexContent(Some(pkg.location), s))
      cmp.meta.code.foreach(c => ContentLocationIndexer.indexContent(Some(pkg.location), c))
    })

    pkg.dependencies.foreach(dep => {
      ContentLocationIndexer.indexContent(Some(pkg.location), dep)
    })
  }

  private class OrderedCompilation(
                                    name:String,
                                    code:Directory,
                                    interface:Directory,
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
          if (path.head.nonEmpty && path.head.charAt(0).isUpper) {
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
        partialPkg = ComponentBuilder.build(nextValue.source, code, interface, partialPkg)
        compiled = compiled + nextKey
      } else {
        feedback(PlainMessage(s"Did not compile ${nextValue.source.identifier} in ${nextValue.source.location} because of errors in dependencies", ErrorManager.Info, Builder()))
        compilationFailed = compilationFailed + nextKey
      }
    }
  }
}
