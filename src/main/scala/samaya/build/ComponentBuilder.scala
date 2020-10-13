package samaya.build

import java.io.DataOutputStream

import samaya.codegen.{ComponentSerializer, ModuleSerializer, NameGenerator}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.unexpected
import samaya.plugin.service.{ComponentValidator, DebugAssembler, InterfaceEncoder, LanguageCompiler, LocationResolver}
import samaya.structure.types.Hash
import samaya.structure.{Component, Meta}
import samaya.types.{Identifier, InputSource, Location}

object ComponentBuilder {
  def build(source:InputSource, codeLoc:Location, interfaceLoc:Location, pkg:PartialPackage): PartialPackage = {
    var partialPkg:PartialPackage = pkg
    //todo: Error Scope
    //todo: Error if not found
    val sourceHash = Hash.fromInputSource(source)
    val finalPkg = LanguageCompiler.compileAndBuildFully(source, partialPkg) { cmp =>
      val invalid = ErrorManager.canProduceErrors{
        //todo: allow to disable
        ComponentValidator.validateComponent(cmp, partialPkg)
      }

      //todo: only when debug output enabled
      produceAsm(codeLoc,cmp,partialPkg)

      val buildRes = if(!invalid) {
        produce(codeLoc, cmp, sourceHash, partialPkg)
      } else {
        None
      }

      val (interfaceInput,interfaceHash) = produceInterface(interfaceLoc, cmp, buildRes.map(_._2), invalid)
      val meta = Meta(
        buildRes.map(_._2),
        interfaceHash,
        sourceHash,
        interfaceInput,
        buildRes.map(_._1),
        Some(source)
      )
      val metaComp = cmp.toInterface(meta)
      partialPkg = partialPkg.withComponent(metaComp)
      (partialPkg, Some(metaComp))
    }

    if(!(finalPkg eq partialPkg)){
      unexpected(s"Compilation for module ${source.identifier} produced stale package")
    }

    partialPkg
  }

  private def produceAsm(code:Location, cmp:Component, pkg:PartialPackage): Unit = {
    //Todo: can we support different debug formats over plugins and set default simular to interface???
    val out = LocationResolver.resolveSink(code, Identifier(NameGenerator.generateCodeName(cmp.name,cmp.classifier),"asm")) match {
      case Some(value) => value
      case None => throw new Exception("Code file output could not be written");//todo: error
    }

    out.write{wOut =>
      val dOut = new DataOutputStream(wOut)
      try {
        DebugAssembler.serializeComponent(pkg,cmp,wOut)
      } finally {
        dOut.close()
      }
    }
  }

  //Todo: detect if uncompilable and skip
  private def produce(code:Location, cmp:Component, sourceHash:Hash, pkg:PartialPackage): Option[(InputSource, Hash)] = {
    if(cmp.isVirtual) return None
    val out = LocationResolver.resolveSink(code, Identifier(NameGenerator.generateCodeName(cmp.name,cmp.classifier),ModuleSerializer.codeExtension)) match {
      case Some(value) => value
      case None => throw new Exception("Code file output could not be written");//todo: error
    }
    val hash = Hash.writeAndHash(out, wOut => {
      val dOut = new DataOutputStream(wOut)
      try {
        ComponentSerializer.serialize(dOut, cmp, sourceHash, pkg)
      } finally {
        dOut.close()
      }
    })
    Some((out.toInputSource, hash))
  }

  private def produceInterface(interface:Location, inter:Component, codeHash:Option[Hash], hasError:Boolean): (InputSource, Hash)  = {
    //todo: do allow sink to select the extension from InterfaceManager
    //todo: get default from config ("json")
    val out = LocationResolver.resolveSink(interface, Identifier(NameGenerator.generateInterfaceName(inter.name,inter.classifier), InterfaceEncoder.interfaceExtensionPrefix+".json")) match {
      case Some(value) => value
      case None => unexpected("Interface file output could not be written");//todo: error
    }
    //todo: check the boolean and throw on false
    val hash = Hash.writeAndHash(out, InterfaceEncoder.serializeInterface(inter, codeHash, hasError, _))
    (out.toInputSource, hash)
  }
}
