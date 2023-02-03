package samaya.validation

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataOutputStream, InputStream, OutputStream}
import java.security.{DigestOutputStream, MessageDigest}
import io.github.rctcwyvrn.blake3.Blake3
import samaya.codegen.{ComponentSerializer, ModuleSerializer, NameGenerator}
import samaya.compilation.ErrorManager
import samaya.structure.{Component, Interface, LinkablePackage, Meta}
import samaya.structure.types.{Blake3OutputStream, Hash}
import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModuleInterface
import samaya.plugin.service.{AddressResolver, ComponentValidator, InterfaceEncoder, LanguageCompiler}
import samaya.types.{Directory, Identifier, InputSource, OutputTarget}

import java.nio.charset.StandardCharsets
import scala.collection.mutable


object PackageValidator {

  case class CacheKey(name:String, language:String, version:String, classifier:Set[String])

  //validate the package integrety
  def validatePackage(pkg: LinkablePackage): Unit = {
    if(pkg.name.isEmpty || pkg.name.charAt(0).isUpper) {
      feedback(PlainMessage("Package names must start with a lowercase Character", Error, Checking()))
    }
    //check that the hash was corectly computed
    validatePackageHash(pkg)
    //have a cache for recompiles
    val recompileCache:mutable.Map[CacheKey, (Boolean,Interface[Component])] = mutable.Map.empty
    val compiled:mutable.Set[InputSource] = mutable.Set.empty
    //process each module
    for (comp <- pkg.components) {
      //Check that the module is valid
      ComponentValidator.validateComponent(comp, pkg)
      //Check that the Hash of the entry was correctly computed
      validateEntryHash(comp.meta, comp.name, "module", pkg, comp.isVirtual)
      //Check that source compiles to byte code & interface
      validateCompilation(comp, pkg, compiled, recompileCache)
    }

  }

  /*
  class MemoryOutPutDebug extends OutputTarget{
    private val out:OutputStream = new OutputStream {
      var count = 0
      override def write(b: Int): Unit = {
        if(count == 603) throw new Exception()
        count += 1
      }
    }
    override def write[T](writer: OutputStream => T): T = try {
      writer(out)
    } finally {
      out.close()
    }

    override def toInputSource: InputSource = null
  }
 */
  //Helpers to have in memory Input/Output
  class MemoryOutPut extends OutputTarget{
    private val out = new ByteArrayOutputStream();
    override def write[T](writer: OutputStream => T): T = try {
      writer(out)
    } finally {
      out.close()
    }

    override def toInputSource: InputSource = new InputSource {
      private val data = out.toByteArray
      override val identifier:Identifier = Identifier(System.identityHashCode(data).toString)
      override def content: InputStream = new ByteArrayInputStream(data)
      override def location: Directory = new Directory {
        override def name: String = "memory"
      }
    }
  }

  //checks that the presented files are really the output of a correct compilation
  // can detect manipulations of code after compilation
  private def validateCompilation(comp: Interface[Component], pkg: LinkablePackage, compiled:mutable.Set[InputSource], recompilationCache:mutable.Map[CacheKey, (Boolean,Interface[Component])]): Unit = {
    //get the code
    val sourceCode = comp.meta.sourceCode match {
      case Some(sc) => sc
      case None =>
        feedback(PlainMessage(s"Can not recompile component ${pkg.location}/${comp.name} because source code is missing", Warning, Checking()))
        return
    }

    if(!compiled.contains(sourceCode)) {
      //recompile the source
      //todo: we should be able to disable the code part if the byte code is not their
      //       Note: we can still compile against it: we can even deploy if the dependencies are already deployed
      LanguageCompiler.compileAndBuildFully(sourceCode,pkg)(ccomp => {
        recompilationCache.get(CacheKey(ccomp.name, ccomp.language, ccomp.version, ccomp.classifier)) match {
          case Some((_,inter)) => (pkg, Some(inter))
          case None =>
            val hasError = ErrorManager.canProduceErrors(ComponentValidator.validateComponent(ccomp, pkg))

            val codeSource = if(ccomp.isVirtual || hasError) {
              None
            } else {
              val out = new MemoryOutPut()
              out.write(wOut => {
                val dOut = new DataOutputStream(wOut)
                try {
                  ComponentSerializer.serialize(dOut, ccomp, sourceCode.hash, pkg)
                } finally {
                  dOut.close()
                }
              })
              Some(out.toInputSource)
            }

            val out = new MemoryOutPut()
            out.write(wOut => {
              val dOut = new DataOutputStream(wOut)
              try {
                InterfaceEncoder.serializeInterface(ccomp,  codeSource.map(_.hash), hasError, dOut)
              } finally {
                dOut.close()
              }
            })
            val interfaceSource = out.toInputSource

            val meta = Meta(
              codeSource.map(_.hash),
              interfaceSource.hash,
              sourceCode.hash,
              interfaceSource,
              codeSource,
              Some(sourceCode)
            )

            val inter = ccomp.toInterface(meta)

            recompilationCache.put(CacheKey(ccomp.name, ccomp.language, ccomp.version, ccomp.classifier),(hasError,inter))
            //we return same package as it does not change in validation mode
            (pkg, Some(inter))
        }
      })
      compiled.add(sourceCode)
    }

    //Note: we do not validate the generated code as we assume the compiler produces correct output
    //      if the compiler does not produce correct output the module can not be deployed
    //        thus an attack can not fake stuff but in the worst case cost money that is wasted
    //        but every sane developer will first deploy into a test net or a simulated local net
    recompilationCache.get(CacheKey(comp.name, comp.language, comp.version, comp.classifier)) match {
      case Some((hasError, ccomp)) =>
        if(hasError){
          feedback(PlainMessage(s"Recompilation for component ${pkg.location}/${comp.name} produced error and can not be deployed", Warning, Checking()))
        }

        //check that the result of the compiler match the values from the entry
        if(!comp.isVirtual) {
          if(comp.meta.codeHash != ccomp.meta.codeHash){
            val cbytes = comp.meta.code.get.content.readAllBytes()
            val ccbytes = ccomp.meta.code.get.content.readAllBytes()
            println(comp.name)
            println(cbytes.length)
            println(ccbytes.length)

            for(((a,b),i) <- cbytes.zip(ccbytes).zipWithIndex){
              if(a != b) {
                println(i+": "+a+" <-> "+b)
              }
            }

            feedback(PlainMessage(s"Recompilation for component ${pkg.location}/${comp.name} produced wrong code hash", Error, Checking()))
          }
        }

        if(comp.meta.interfaceHash != ccomp.meta.interfaceHash){
          feedback(PlainMessage(s"Recompilation for component ${pkg.location}/${comp.name} produced wrong interface hash", Error, Checking()))
        }

      case None =>
        feedback(PlainMessage(s"Recompilation for module ${pkg.location}/${comp.name} failed", Error, Checking()))
    }
  }

  //validate the 3 different hashes for the 3 different categories
  private def validatePackageHash(pkg: LinkablePackage): Unit = {
    val digest = Blake3.newInstance
    pkg.components.map(e => e.meta.interfaceHash).sorted.distinct.foreach(h => digest.update(h.data))

    pkg.dependencies.sortBy(p => p.name).distinct.foreach(p => {
      digest.update(p.name.getBytes)
      digest.update(p.hash.data)
    })

    if(pkg.hash != Hash.fromBytes(digest.digest(Hash.byteLen))){
      feedback(PlainMessage(s"Integrity for package ${pkg.location} violated", Error, Checking()))
    }
  }

  //validate the 3 different hashes for all the entries
  private def validateEntryHash(meta: Meta, name:String, kind:String, pkg: LinkablePackage, isVirtual:Boolean): Unit = {
    validateEntryInterface(meta, name, kind, pkg)
    if(!isVirtual) validateEntryCode(meta, name, kind, pkg)
    validateEntrySource(meta, name, kind,  pkg)
  }

  //Hash the byte code file and compare it ti the recorded hash in the entry
  private def validateEntryCode(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    meta.code match {
      case Some(code) =>
        if(!meta.codeHash.contains(code.hash)) {
          feedback(PlainMessage(s"Code source for $kind ${pkg.location}/$name produced wrong code hash", Error, Checking()))
        }
      case None => feedback(PlainMessage(s"Can not check code integrity of entry ${pkg.location}/$name as code source is missing", Warning, Checking()))
    }
  }

  //Hash the source code file and compare it ti the recorded hash in the entry
  private def validateEntrySource(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    meta.sourceCode match {
      case Some(sourceCode) =>
        if(meta.sourceHash != Hash.fromInputSource(sourceCode)) {
          feedback(PlainMessage(s"Source code source for $kind ${pkg.location}/$name produced wrong source code hash", Error, Checking()))
        }
      case None => feedback(PlainMessage(s"Can not check source code integrity of entry ${pkg.location}/${name} as source code source is missing", Warning, Checking()))
    }
  }

  //Hash the interface file and compare it ti the recorded hash in the entry
  private def validateEntryInterface(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    if(meta.interfaceHash != meta.interface.hash){
      feedback(PlainMessage(s"Interface source for $kind ${pkg.location}/$name produced wrong interface hash", Error, Checking()))
    }
  }

}