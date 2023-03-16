package samaya.build


import samaya.build.HelperTool.withRepos
import samaya.build.desc.Dependency
import samaya.compilation.ErrorManager
import samaya.plugin.service.{AddressResolver, ContentLocationIndexer, DependenciesImportSourceEncoder, LanguageCompiler, PackageEncoder, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types.{Address, Directory, Identifier, InputSource, Workspace}
import samaya.validation.WorkspaceValidator
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, PlainMessage, Warning, canProduceErrors, feedback, unexpected}
import samaya.plugin.impl.pkg.json.JsonModel.Info
import samaya.plugin.impl.wp.json.WorkspaceImpl
import samaya.plugin.shared.repositories.Repositories.Repository


object BuildTool {

  def main(args: Array[String]): Unit = build(args(0))

  def build(target:String):Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()
    val wp = HelperTool.createWorkspace(target);
    //todo: Have some default Repositories loaded from a file
    val res = compileWorkspace(wp)
    ContentLocationIndexer.storeIndex(wp.workspaceLocation)
    println(s"compilation of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
    res
  }

  private case class Job(source:InputSource, dependencies:Set[Dependency])

  //Todo: if we do these to does pack them into helper & share
  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff?
  private def compileWorkspace(wp:Workspace):Option[LinkablePackage] = {
    val depsBuilder = Map.newBuilder[String, LinkablePackage]
    val includes =  wp.includes.getOrElse(Set.empty)
    var repos = wp.repositories.getOrElse(Set.empty)
    val dependencies =  wp.dependencies.getOrElse(Set.empty)

    depsBuilder.sizeHint(includes.size + dependencies.size)
    //Todo: is this needed here or is it sufficient to have in WorkspaceEncoder
    withRepos(repos){
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
  }

  def updateContentIndexes(pkg:LinkablePackage): Unit = {

    ContentLocationIndexer.indexContent(pkg)

    pkg.components.foreach(cmp => {
      ContentLocationIndexer.indexContent(cmp.meta.interface)
      cmp.meta.sourceCode.foreach(s => ContentLocationIndexer.indexContent(s))
      cmp.meta.code.foreach(c => ContentLocationIndexer.indexContent(c))
    })

    pkg.dependencies.foreach(dep => {
      ContentLocationIndexer.indexContent(dep)
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
