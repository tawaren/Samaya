package samaya.validation

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataOutputStream, InputStream, OutputStream}
import samaya.codegen.ComponentSerializer
import samaya.compilation.ErrorManager
import samaya.structure.{Component, Interface, LinkablePackage, Meta}
import samaya.structure.types.Hash
import samaya.compilation.ErrorManager._
import samaya.plugin.service.{AddressResolver, ComponentValidator, InterfaceEncoder, LanguageCompiler}
import samaya.toolbox.Crypto
import samaya.types.Address.ContentBased
import samaya.types.{Directory, Identifier, InputSource}

import scala.collection.mutable
import scala.util.Using


object PackageValidator {

  case class CacheKey(name:String, language:String, version:String, classifier:Set[String])

  //validate the package integrety
  def validatePackage(pkg: LinkablePackage, recursive:Boolean =false): Unit = {
    if(pkg.name.isEmpty || pkg.name.charAt(0).isUpper) {
      feedback(PlainMessage("Package names must start with a lowercase Character", Error, Checking()))
    }
    //Todo: Parallelize - maybe validateCompilation sequential at first?
    //       or make the caches concurrent
    //check that the hash was correctly computed
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

    if(recursive){
      for(dep <- pkg.dependencies){
        validatePackage(dep, true)
      }
    }

  }

  def inputFromStream(f: OutputStream => Unit): InputSource = {
    val out = new ByteArrayOutputStream()
    f(out)
    out.close()
    new InputSource {
      private val data = out.toByteArray
      override def read[T](reader: InputStream => T): T = Using(new ByteArrayInputStream(data))(reader).get
      override def identifier:Identifier  = Identifier.Specific(System.identityHashCode(data).toString)
      override def location: Directory = unexpected("In memory sources do not have a location", Always)
    }
  }

  //checks that the presented files are really the output of a correct compilation
  // can detect manipulations of code after compilation
  private def validateCompilation(comp: Interface[Component], pkg: LinkablePackage, compiled:mutable.Set[InputSource], recompilationCache:mutable.Map[CacheKey, (Boolean,Interface[Component])]): Unit = {
    //get the code
    val sourceCode = comp.meta.sourceCode match {
      case Some(sc) => sc
      case None => AddressResolver.resolve(ContentBased(comp.meta.sourceHash), AddressResolver.InputLoader) match {
        case Some(sc) => sc
        case None =>
          feedback(PlainMessage(s"Can not recompile component ${pkg.location}/${comp.name} because source code is missing", Warning, Checking()))
          return
      }
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
              Some(inputFromStream(wOut => {
                val dOut = new DataOutputStream(wOut)
                try {
                  ComponentSerializer.serialize(dOut, ccomp, sourceCode.hash, pkg)
                } finally {
                  dOut.flush()
                }
              }))
            }

            val interfaceSource = inputFromStream(wOut => {
              val dOut = new DataOutputStream(wOut)
              try {
                InterfaceEncoder.serializeInterface(ccomp,  codeSource.map(_.hash), hasError, dOut)
              } finally {
                dOut.flush()
              }
            })

            val meta = Meta(
              codeSource.map(_.hash),
              interfaceSource.hash,
              sourceCode.hash,
              Some(interfaceSource),
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
    val digest = Crypto.newHasher()
    pkg.components.map(e => e.meta.interfaceHash).sorted.distinct.foreach(h => digest.update(h.data))

    pkg.dependencies.sortBy(p => p.name).distinct.foreach(p => {
      digest.update(p.name.getBytes)
      digest.update(p.hash.data)
    })

    if(pkg.hash != Hash.fromBytes(digest.finalize(Hash.byteLen))){
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
      //Note: This is enough as content addressing guarantees the hash is right
      case None => meta.codeHash match {
        case Some(hash) if AddressResolver.resolve(ContentBased(hash), AddressResolver.InputLoader).isDefined =>
        case _ => feedback(PlainMessage(s"Can not check code integrity of entry ${pkg.location}/$name as code source is missing", Warning, Checking()))
      }
    }
  }

  //Hash the source code file and compare it ti the recorded hash in the entry
  private def validateEntrySource(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    meta.sourceCode match {
      case Some(sourceCode) =>
        if(meta.sourceHash != Hash.fromInputSource(sourceCode)) {
          feedback(PlainMessage(s"Source code source for $kind ${pkg.location}/$name produced wrong source code hash", Error, Checking()))
        }
      //Note: This is enough as content addressing guarantees the hash is right
      case None => if(AddressResolver.resolve(ContentBased(meta.sourceHash), AddressResolver.InputLoader).isEmpty) {
        feedback(PlainMessage(s"Can not check source code integrity of entry ${pkg.location}/${name} as source code source is missing", Warning, Checking()))
      }
    }
  }

  //Hash the interface file and compare it ti the recorded hash in the entry
  private def validateEntryInterface(meta: Meta, name:String, kind:String, pkg: LinkablePackage): Unit = {
    meta.interface match {
      case Some(interfaceCode) =>
        if(meta.interfaceHash != Hash.fromInputSource(interfaceCode)) {
          feedback(PlainMessage(s"Interface source for $kind ${pkg.location}/$name produced wrong interface hash", Error, Checking()))
        }
      //Note: This is enough as content addressing guarantees the hash is right
      case None => if(AddressResolver.resolve(ContentBased(meta.interfaceHash), AddressResolver.InputLoader).isEmpty) {
        feedback(PlainMessage(s"Can not check interface integrity of entry ${pkg.location}/${name} as interface source is missing", Warning, Checking()))
      }
    }
  }

}
