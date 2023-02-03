package samaya.build

import java.io.DataOutputStream

import samaya.codegen.{ComponentSerializer, ModuleSerializer, NameGenerator}
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{Builder, unexpected}
import samaya.plugin.service.{ComponentValidator, DebugAssembler, InterfaceEncoder, LanguageCompiler, AddressResolver}
import samaya.structure.types.Hash
import samaya.structure.{Component, Meta}
import samaya.types.{Identifier, InputSource, Directory}

object ComponentBuilder {


  def build(source:InputSource, codeLoc:Directory, interfaceLoc:Directory, pkg:PartialPackage): PartialPackage = {
    var partialPkg:PartialPackage = pkg
    //todo: Error Scope
    //todo: Error if not found
    val finalPkg = LanguageCompiler.compileAndBuildFully(source, partialPkg) { cmp =>
      val invalid = ErrorManager.canProduceErrors{
        //todo: allow to disable
        ComponentValidator.validateComponent(cmp, partialPkg)
      }

      val buildRes = if(!invalid) {
        produce(codeLoc, cmp, source.hash, partialPkg)
      } else {
        None
      }

      produceAsm(codeLoc,cmp,partialPkg)

      val interfaceInput = produceInterface(interfaceLoc, cmp, buildRes.map(_.hash), invalid)

      val meta = Meta(
        buildRes.map(_.hash),
        interfaceInput.hash,
        source.hash,
        interfaceInput,
        buildRes,
        Some(source)
      )
      val metaComp = cmp.toInterface(meta)
      partialPkg = partialPkg.withComponent(metaComp)
      (partialPkg, Some(metaComp))
    }

    if(!(finalPkg eq partialPkg)){
      unexpected(s"Compilation for module ${source.identifier} produced stale package", Builder())
    }

    partialPkg
  }

  private def produceAsm(code:Directory, cmp:Component, pkg:PartialPackage): Unit = {
    //Todo: can we support different debug formats over plugins and set default simular to interface???
    val out = AddressResolver.resolveSink(code, Identifier(NameGenerator.generateCodeName(cmp.name,cmp.classifier),"asm")) match {
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
  private def produce(code:Directory, cmp:Component, sourceHash:Hash, pkg:PartialPackage): Option[InputSource] = {
    if(cmp.isVirtual) return None
    val out = AddressResolver.resolveSink(code, Identifier(NameGenerator.generateCodeName(cmp.name,cmp.classifier),ModuleSerializer.codeExtension)) match {
      case Some(value) => value
      case None => throw new Exception("Code file output could not be written");//todo: error
    }

    out.write(wOut => {
      val dOut = new DataOutputStream(wOut)
      try {
        ComponentSerializer.serialize(dOut, cmp, sourceHash, pkg)
      } finally {
        dOut.close()
      }
    })
    Some(out.toInputSource)
  }

  private def produceInterface(interface:Directory, inter:Component, codeHash:Option[Hash], hasError:Boolean): InputSource = {
    //todo: do allow sink to select the extension from InterfaceManager
    //todo: get default from config ("json")
    val out = AddressResolver.resolveSink(interface, Identifier(NameGenerator.generateInterfaceName(inter.name,inter.classifier), InterfaceEncoder.interfaceExtensionPrefix+".json")) match {
      case Some(value) => value
      case None => unexpected("Interface file output could not be written", Builder());//todo: error
    }
    //todo: check the boolean and throw on false
    out.write(wOut => {
      val dOut = new DataOutputStream(wOut)
      try {
        InterfaceEncoder.serializeInterface(inter, codeHash, hasError, dOut)
      } finally {
        dOut.close()
      }
    })
    out.toInputSource
  }
}
