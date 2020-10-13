package samaya.validation

import java.io.DataOutputStream
import java.security.{DigestOutputStream, MessageDigest}

import com.rfksystems.blake2b.Blake2b
import samaya.codegen.ComponentSerializer
import samaya.compilation.ErrorManager
import samaya.structure.{Component, Interface, LinkablePackage, Meta}
import samaya.structure.types.Hash
import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModuleInterface
import samaya.plugin.service.{ComponentValidator, InterfaceEncoder, LanguageCompiler}
import samaya.types.InputSource

import scala.collection.mutable


object PackageValidator {

  case class CacheKey(name:String, language:String, version:String, classifier:Set[String])

  //validate the package integrety
  def validatePackage(pkg: LinkablePackage): Unit = {
    if(pkg.name.length == 0 || pkg.name.charAt(0).isUpper) {
      feedback(PlainMessage("Package names must start with a lowercase Character", Error))
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
      //Check that the Hash of the entry was correcty computed
      validateEntryHash(comp.meta, comp.name, "module", pkg)
      //Check that source compiles to byte code & interface
      validateCompilation(comp, pkg, compiled, recompileCache)
    }

  }
  //checks that the presented files are really the output of a correct compilation
  // can detect manipulations of code after compilation
  private def validateCompilation(comp: Interface[Component], pkg: LinkablePackage, compiled:mutable.Set[InputSource], recompilationCache:mutable.Map[CacheKey, (Boolean,Interface[Component])]): Unit = {
    //get the code
    val sourceCode = comp.meta.sourceCode match {
      case Some(sc) => sc
      case None =>
        feedback(PlainMessage(s"Can not recompile component ${pkg.location}/${comp.name} because source code is missing", Warning))
        return
    }

    if(!compiled.contains(sourceCode)) {
      //recompile the source
      //todo: we should be able to disable the code part if the byte code is not their
      //       Note: we can still compile against it: we can even deploy if the dependencies are already deployed
      LanguageCompiler.compileAndBuildFully(sourceCode,pkg)(ccomp => {
        val hasError = ErrorManager.canProduceErrors(ComponentValidator.validateComponent(ccomp, pkg))
        val inter = ccomp.toInterface(comp.meta)
        recompilationCache.put(CacheKey(ccomp.name, ccomp.language, ccomp.version, ccomp.classifier),(hasError,inter))
        //we return same package as it does not change in validation mode
        (pkg, Some(inter))
      })
      compiled.add(sourceCode)
    }

    //Note: we do not validate the generated code as we assume the compiler produces correct output
    //      if the compiler does not produce correct output the module can not be deployed
    //        thus an attack can not fake stuff but in the worst case cost money that is wasted
    //        but every sane developer will first deploy into a test net or a simulated local net
    recompilationCache.get(CacheKey(comp.name, comp.language, comp.version, comp.classifier)) match {
      case Some((hasError, ccomp)) =>
        //check that the result of the compiler match the values from the entry
        //todo: we should be able to disable the code part if the byte code is not their
        //       Note: we can still compile against it: we can even deploy if the dependencies are already deployed
        if(!comp.isVirtual) {
          validateCompiledCode(comp, pkg, ccomp)
        }

        //todo: what if we did not produce code
        //      We should still be able to generate the interface but currently we are not
        validateCompiledInterface(comp, pkg, hasError, ccomp)
      case None =>
        feedback(PlainMessage(s"Recompilation for module ${pkg.location}/${comp.name} failed", Error))
    }
  }

  //validate the 3 different hashes for the 3 different categories
  private def validatePackageHash(pkg: LinkablePackage): Unit = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
    pkg.components.map(e => e.meta.interfaceHash).sorted.distinct.foreach(h => digest.update(h.data))

    pkg.dependencies.sortBy(p => p.name).distinct.foreach(p => {
      digest.update(p.name.getBytes)
      digest.update(p.hash.data)
    })

    if(pkg.hash != Hash.fromBytes(digest.digest())){
      feedback(PlainMessage(s"Integrity for package ${pkg.location} violated", Error))
    }
  }

  //validate the 3 different hashes for all the entries
  private def validateEntryHash(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    validateEntryInterface(meta, name, kind, pkg)
    validateEntryCode(meta, name, kind, pkg)
    validateEntrySource(meta, name, kind,  pkg)
  }

  //Hash the code and compare it to the recorded hash in the entry
  private def validateCompiledCode(component: Interface[Component], pkg: LinkablePackage, ccomp: Component): Unit = {
    component.meta.codeHash match {
      case Some(hash) =>
        val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
        val stream = new DataOutputStream( new DigestOutputStream((_: Int) => {}, digest))
        ComponentSerializer.serialize(stream, ccomp, component.meta.sourceHash, pkg)
        stream.close()
        if(hash != Hash.fromBytes(digest.digest())) {
          feedback(PlainMessage(s"Recompilation for component ${pkg.location}/${component.name} produced wrong code hash", Error))
        }
      case None => //Nothing to do for code less components
    }

  }

  //Hash the interface and compare it ti the recorded hash in the entry
  private def validateCompiledInterface(comp: Interface[Component], pkg: LinkablePackage, hasError:Boolean, ccomp: Interface[Component]): Unit = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
    val stream = new DataOutputStream( new DigestOutputStream((_: Int) => {}, digest))
    if(!InterfaceEncoder.serializeInterface(ccomp, comp.meta.codeHash, hasError, stream)){
      stream.close()
      feedback(PlainMessage(s"Can not check interface for component ${pkg.location}/${comp.name} because interface serializer is missing", Warning))
    } else {
      stream.close()
      if(comp.meta.interfaceHash != Hash.fromBytes(digest.digest())){
        feedback(PlainMessage(s"Recompilation for component ${pkg.location}/${comp.name} produced wrong interface hash", Error))
      }
    }
  }

  //Hash the byte code file and compare it ti the recorded hash in the entry
  private def validateEntryCode(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    meta.code match {
      case Some(code) =>
        val hash = Hash.fromInputSource(code)
        if(!meta.codeHash.contains(hash)) {
          feedback(PlainMessage(s"Code source for $kind ${pkg.location}/$name produced wrong code hash", Error))
        }
      case None => feedback(PlainMessage(s"Can not check code integrity of entry ${pkg.location}/$name as code source is missing", Warning))
    }
  }

  //Hash the source code file and compare it ti the recorded hash in the entry
  private def validateEntrySource(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    meta.sourceCode match {
      case Some(sourceCode) =>
        if(meta.sourceHash != Hash.fromInputSource(sourceCode)) {
          feedback(PlainMessage(s"Source code source for $kind ${pkg.location}/$name produced wrong source code hash", Error))
        }
      case None => feedback(PlainMessage(s"Can not check source code integrity of entry ${pkg.location}/${name} as source code source is missing", Warning))
    }
  }

  //Hash the interface file and compare it ti the recorded hash in the entry
  private def validateEntryInterface(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    if(meta.interfaceHash != Hash.fromInputSource(meta.interface)){
      feedback(PlainMessage(s"Interface source for $kind ${pkg.location}/$name produced wrong interface hash", Error))
    }
  }

}