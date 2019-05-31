package mandalac.validation

import java.io.{DataOutputStream, InputStream}
import java.security.{DigestInputStream, DigestOutputStream, MessageDigest, Security}

import com.rfksystems.blake2b.Blake2b
import com.rfksystems.blake2b.security.Blake2bProvider
import mandalac.registries.ModuleRegistry
import mandalac.structure.Module
import mandalac.structure.types.Hash
import mandalac.compilation.ErrorHandler._
import mandalac.plugin.service.{InterfaceManager, LanguageCompiler}
import mandalac.types.Package


object PackageValidator {

  //We need to do some hashing ensure blake is available
  Security.addProvider(new Blake2bProvider)

  //validate the package integrety
  def validatePackage(pkg: Package): Unit = {
    //check that the hash was corectly computed
    validatePackageHash(pkg)
    //process each module
    for (module <- pkg.modules) {
      //Check that the Hash of the entry was correcty computed
      validateEntryHash(module, pkg)
      //Makes some integrety checks for each entry
      validateEntry(module, pkg)
      //Check that source compiles to byte code & interface
      validateCompilation(module, pkg)
    }
  }

  //checks that the registered module matches the entry
  private def validateEntry(module: Module, pkg: Package): Unit = {
    //get the registered interface module
    //todo: add classifier
    val regModule = ModuleRegistry.moduleByName(pkg.path, module.name, module.classifier) match {
      case Some(c) => c
      case None => unexpected("Interface for module is missing")
    }

    //does the hash of the module match the hash of the entry?
    if (module.hash != regModule.hash) unexpected("Code hash mismatch")
    //does the hash of the module match the hash of the entry?
    if (module.meta.interfaceHash != regModule.meta.interfaceHash) unexpected("Interface hash mismatch")
    //does the hash of the module match the hash of the entry?
    if (module.meta.sourceHash != regModule.meta.sourceHash) unexpected("Source hash mismatch")

    //does the name of the module match the name of the entry?
    if (module.name != regModule.name) unexpected("Interface name mismatch")
    //does the language of the module match the language of the entry?
    if (module.language != regModule.language) unexpected("Interface language mismatch")
    //does the classifier of the module match the classifier of the entry?
    if (module.classifier != regModule.classifier) unexpected("Interface classifier mismatch")
    //does the version of the module match the version of the entry?
    if (module.version == regModule.version) unexpected("Interface version mismatch")
  }

  //checks that the presented files are really the output of a correct compilation
  // needed to detect manipulations of code after compilation
  private def validateCompilation(module: Module, pkg: Package): Unit = {

    //get the code
    val sourceCode = module.meta.sourceCode match {
      case Some(sc) => sc
      case None =>
        feedback(SimpleMessage(s"Can not recompile module ${pkg.path}/${module.name} because source code is missing", Warning))
        return
    }

    //recompile the source
    val compModule = LanguageCompiler.compileSpecific(module.language, module.version, module.classifier, sourceCode, pkg) match {
      case Some(m) =>
        ModuleValidator.validateModule(m)
        m
      case None =>
        //todo: check if it was the compiler that was missing and if so give another error
        feedback(SimpleMessage(s"Recompilation for module ${pkg.path}/${module.name} failed", Error))
        return
    }
    //Note: we do not validate the generated code as we assume the compiler produces correct output
    //      if the compiler does not produce correct output the module can not be deployed
    //        thus a attack can not fake stuff but in the worst case cost money that is wasted
    //        but every sane developer will first deploy into a test net or a simulated local net

    //check that the result of the compiler match the values from the entry
    //todo: we should be able to disable the code part if the byte code is not their
    //       Note: we can still compile against it: we can even deploy if the dependencies are already deployed
    validateCompiledCode(module, pkg, compModule)
    validateCompiledInterface(module, pkg, compModule)
  }


  //validate the 3 different hashes for the 3 different categories
  private def validatePackageHash(pkg: Package): Unit = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
    pkg.modules.map(e => e.meta.interfaceHash).sorted.distinct.foreach(h => digest.update(h.data))
    pkg.dependencies.sortBy(p => p.name).distinct.foreach(p => {
      digest.update(p.name.getBytes)
      digest.update(p.hash.data)
    })
    if(pkg.hash != Hash.fromBytes(digest.digest())){
      feedback(SimpleMessage(s"Integrity for package ${pkg.path} violated", Error))
    }
  }

  //validate the 3 different hashes for all the entries
  private def validateEntryHash(module: Module, pkg: Package): Unit = {
    validateEntryInterface(module, pkg)
    validateEntryCode(module, pkg)
    validateEntrySource(module, pkg)
  }

  //Todo: Strange -- Needed How todo
  //Hash the code and compare it ti the recorded hash in the entry
  private def validateCompiledCode(module: Module, pkg: Package, compModule: Module): Unit = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
    val stream = new DataOutputStream( new DigestOutputStream((_: Int) => {}, digest))
    compModule.serialize(stream)
    stream.close()
    if(module.hash != Hash.fromBytes(digest.digest())) {
      feedback(SimpleMessage(s"Recompilation for module ${pkg.path}/${module.name} produced wrong code hash", Error))
    }
  }

  //Hash the interface and compare it ti the recorded hash in the entry
  private def validateCompiledInterface(module: Module, pkg: Package, compModule: Module): Unit = {
    val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
    val stream = new DataOutputStream( new DigestOutputStream((_: Int) => {}, digest))
    if(!InterfaceManager.serializeInterface(compModule, stream)){
      stream.close()
      feedback(SimpleMessage(s"Can not check interface for module ${pkg.path}/${module.name} because interface serializer is missing", Warning))
    } else {
      stream.close()
      if(module.meta.interfaceHash != Hash.fromBytes(digest.digest())){
        feedback(SimpleMessage(s"Recompilation for module ${pkg.path}/${module.name} produced wrong interface hash", Error))
      }
    }
  }

  //Helper to directly Hash an InputStream
  private def hashInput(input: InputStream): Array[Byte] = {
      val digest = MessageDigest.getInstance(Blake2b.BLAKE2_B_160)
      val digestStream = new DigestInputStream(input, digest)
      while (digestStream.read() > -1) {}
      digestStream.close()
      digest.digest()
  }


  //Hash the byte code file and compare it ti the recorded hash in the entry
  private def validateEntryCode(module:Module, pkg: Package): Unit = {
    module.meta.code match {
      case Some(code) =>
        if(module.hash != Hash.fromBytes(hashInput(code.content))) {
          feedback(SimpleMessage(s"Code source for module ${pkg.path}/${module.name} produced wrong code hash", Error))
        }
      case None => feedback(SimpleMessage(s"Can not check code integrity of entry ${pkg.path}/${module.name} as code source is missing", Warning))
    }
  }

  //Hash the source code file and compare it ti the recorded hash in the entry
  private def validateEntrySource(module:Module, pkg: Package): Unit = {
    module.meta.sourceCode match {
      case Some(sourceCode) =>
        if(module.meta.sourceHash != Hash.fromBytes(hashInput(sourceCode.content))) {
          feedback(SimpleMessage(s"Source code source for module ${pkg.path}/${module.name} produced wrong source code hash", Error))
        }
      case None => feedback(SimpleMessage(s"Can not check source code integrity of entry ${pkg.path}/${module.name} as source code source is missing", Warning))
    }
  }

  //Hash the interface file and compare it ti the recorded hash in the entry
  private def validateEntryInterface(module: Module, pkg: Package): Unit = {
    if(module.meta.interfaceHash != Hash.fromBytes(hashInput(module.meta.interface.content))){
      feedback(SimpleMessage(s"Interface source for module ${pkg.path}/${module.name} produced wrong interface hash", Error))
    }
  }

}