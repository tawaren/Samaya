package samaya.build


import samaya.build.compilation.{Compilation, Dependency}
import samaya.compilation.ErrorManager
import samaya.plugin.service.{AddressResolver, ContentLocationIndexer, LanguageCompiler, PackageEncoder, WorkspaceEncoder}
import samaya.structure.LinkablePackage
import samaya.types.{Address, Directory, InputSource, Repository, Workspace}
import samaya.compilation.ErrorManager.{Builder, Error, LocatedMessage, PlainMessage, canProduceErrors, feedback}

object BuildTool {

  def main(args: Array[String]): Unit = build(args(0))

  def build(target:String):Option[LinkablePackage] = {
    val t0 = System.currentTimeMillis()
    val wp = HelperTool.createWorkspace(target);
    //todo: Have some default Repositories loaded from a file
    val res = compileWorkspace(wp)
    ContentLocationIndexer.storeIndex(wp.location)
    println(s"compilation of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
    res
  }

  //Todo: if we do these to does pack them into helper & share
  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff?
  private def compileWorkspace(wp:Workspace):Option[LinkablePackage] = {
    val depsBuilder = Map.newBuilder[String, LinkablePackage]
    val includes =  wp.includes.getOrElse(Set.empty)
    val repos = wp.repositories.getOrElse(Set.empty)
    val dependencies =  wp.dependencies.getOrElse(Set.empty)

    depsBuilder.sizeHint(includes.size + dependencies.size)
    Repository.withRepos(repos){
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
            AddressResolver.resolve(source, src, AddressResolver.InputLoader) match {
              case None =>
                println(source)
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
          (compSource.identifier.name,Compilation.Job(compSource, LanguageCompiler.extractDependencies(compSource)))
        ).toMap

        val nameFileMapping = compSources.flatMap(moduleSource =>
          LanguageCompiler.extractComponentNames(moduleSource).map(dep => (dep, moduleSource.identifier.name))
        ).toMap

        //Todo: Make A Compilation Scheduler Plugin
        val state = Compilation.parallel(wp.name, code, interface, resolvedDeps, nameFileMapping, uncompiled)

        val resPkg = state.compileAll().toLinkablePackage(wp.location)
        PackageEncoder.serializePackage(resPkg, wp)
        updateContentIndexes(resPkg)
        println(s"compilation of package ${resPkg.name} finished in ${System.currentTimeMillis()-t0} ms" )
        Some(resPkg)
      } else {
        feedback(PlainMessage(s"Did not compile ${wp.name} in ${wp.location} because of errors in dependencies", ErrorManager.Info, Builder()))
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
}
