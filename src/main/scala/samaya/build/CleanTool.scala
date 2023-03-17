package samaya.build

import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Builder, PlainMessage, canProduceErrors, feedback}
import samaya.plugin.service.AddressResolver
import samaya.types.Workspace

object CleanTool {

  def main(args: Array[String]): Unit = clean(args(0))

  //Todo: Abstract is nearly identical to compile
  def clean(target:String):Unit = {
    val t0 = System.currentTimeMillis()
    val wp = HelperTool.createWorkspace(target);
    cleanWorkspace(wp)
    println(s"cleaning of workspace ${wp.name} finished in ${System.currentTimeMillis()-t0} ms" )
  }

  //Todo: if we do these todoes pack them into helper & share
  //todo: the getOrElse(Set.empty) need an infer by convention algorithm
  //       a plugin that has a strategy to find missing stuff
  private def cleanWorkspace(wp:Workspace):Unit =  {
    //todo: look for a default location (probably the include folder in parent)
    //todo:  or search subdirectories for workspace files
    val includes =  wp.includes.getOrElse(Set.empty)

    val depsError = canProduceErrors{
      includes.foreach(cleanWorkspace)
    }

    //Todo: Fails - Fix
    if(!depsError) {
      //Can we be more gentle??
      AddressResolver.deleteDirectory(wp.codeLocation)
      AddressResolver.deleteDirectory(wp.interfaceLocation)
      //Todo: delete package files
    } else {
      feedback(PlainMessage(s"Did not clean ${wp.name} in ${wp.location} because of errors in dependencies", ErrorManager.Info, Builder()))
    }
  }
}
