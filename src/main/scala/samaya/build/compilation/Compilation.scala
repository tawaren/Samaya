package samaya.build.compilation

import samaya.build.PartialPackage
import samaya.structure.LinkablePackage
import samaya.types.{Directory, InputSource}

//Todo: Make Configurable
trait Compilation {
  def compileAll():PartialPackage[_]
}

object Compilation {
  case class Job(source:InputSource, dependencies:Set[Dependency])

  def sequential(name:String,code:Directory,interface:Directory,resolvedDeps:Map[String, LinkablePackage], nameFileMapping:Map[String,String], uncompiled:Map[String, Job]): Compilation ={
    new SequentialCompilation(name,code,interface,resolvedDeps,nameFileMapping,uncompiled)
  }

  //Todo: Note this is only marginally faster as we still have a lot of IO
  //      As soon as we can switch to Virtual Threads this should become better
  def parallel(name:String,code:Directory,interface:Directory,resolvedDeps:Map[String, LinkablePackage], nameFileMapping:Map[String,String], uncompiled:Map[String, Job]): Compilation ={
    new ParallelCompilation(name,code,interface,resolvedDeps,nameFileMapping,uncompiled)
  }
}