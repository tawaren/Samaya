package samaya.plugin.impl.compiler.mandala.inter.json

import java.io.OutputStream

import com.github.plokhotnyuk.jsoniter_scala.core.{WriterConfig, readFromStream, writeToStream}
import samaya.compilation.ErrorManager.{PlainMessage, Warning, feedback, unexpected}
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.{clazz, module}
import JsonModel.{Alias, InstanceEntry, InterfaceFunClass, InterfaceInstance, InterfaceMandalaModule, InterfaceSigClass}
import samaya.plugin.impl.compiler.mandala.components.clazz.{ClassInterface, FunClass, FunClassInterface, SigClass, SigClassInterface}
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.{LocalEntryRef, RemoteEntryRef}
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, DefInstanceInterface, Instance, InstanceInterface}
import samaya.plugin.impl.compiler.mandala.components.module.{MandalaModule, MandalaModuleInterface}
import samaya.plugin.impl.inter.json.Serializer.{toDataTypeRepr, toFunctionRepr, toGenericRepr}
import samaya.plugin.impl.inter.json.{JsonLocation, Serializer}
import samaya.plugin.service.{InterfaceEncoder, Selectors}
import samaya.structure.Module.Mode
import samaya.structure.types.{CompLink, Hash}
import samaya.structure.{Component, Interface, Meta}
import samaya.types.InputSource

//A Interface Manager for a json description of a interface
class JsonInterfaceEncoder extends InterfaceEncoder {

  val Json = "json"

  override def matches(s: Selectors.InterfaceSelector): Boolean =  {
    def check(classifiers:Set[String]) = {
      MandalaCompiler.MandalaModule_Classifier.subsetOf(classifiers) ||
        MandalaCompiler.DefInstance_Classifier.subsetOf(classifiers) ||
        MandalaCompiler.FunClass_Classifier.subsetOf(classifiers) ||
        MandalaCompiler.SigClass_Classifier.subsetOf(classifiers)
    }

    s match {
      case Selectors.InterfaceDeserializationSelector(MandalaCompiler.Language,_,classifier,InterfaceExtension(Json)) => check(classifier)
      case Selectors.InterfaceSerializationSelector(MandalaCompiler.Language,_,classifier,InterfaceExtension(Json)) => check(classifier)
      case _ => false
    }
  }

  override def serializeInterface(inter:Component, codeHash:Option[Hash], hasError:Boolean, out:OutputStream): Boolean = {
    inter match {
      case module:MandalaModule => serializeMandalaModule(module, codeHash, hasError, out)
      case cls:clazz.SigClass => serializeSigClass(cls, codeHash, hasError, out)
      case inst: DefInstance => serializeInstanceInterface(inst, hasError, out)
      case cls:clazz.FunClass => serializeFunClass(cls, hasError, out)
      case other => unexpected(s"Component $other can not be used in interface gen")
    }
  }

  private def serializeInstanceInterface(inst: DefInstance, hasError:Boolean, out: OutputStream): Boolean = {
    val repr = InterfaceInstance(
      name = inst.name,
      hadError = hasError,
      language = inst.language,
      version = inst.version,
      classifier = inst.classifier,
      classTarget = inst.classTarget match {
        case CompLink.ByCode(_) => unexpected("Class targets should refer to classes");
        case CompLink.ByInterface(hash) => hash.toString
      },
      applies = inst.applies.map(Serializer.toTypeRepr),
      funAliases = inst.funReferences.map{
        case (name,RemoteEntryRef(module, offset)) => Alias(name, JsonModel.EntryRef(module.toString, offset))
        case (_,LocalEntryRef(_)) => unexpected("Instances are not modules and can not have self references")
      }.toSeq,
      implAliases = inst.implReferences.map{
        case (name,RemoteEntryRef(module, offset)) => Alias(name, JsonModel.EntryRef(module.toString, offset))
        case (_,LocalEntryRef(_)) => unexpected("Instances are not modules and can not have self references")
      }.toSeq
    )
    writeToStream[InterfaceInstance](repr, out, WriterConfig(indentionStep = 2))
    true
  }

  private def serializeMandalaModule(module:MandalaModule, codeHash:Option[Hash], hasError:Boolean, out: OutputStream): Boolean = {
    assert(module.signatures.isEmpty)
    val functions = module.functions.map(toFunctionRepr)
    val implements = module.implements.map(toFunctionRepr)
    val datatypes = module.dataTypes.map(toDataTypeRepr)
    val instances = module.instances.map {
      case (CompLink.ByInterface(hash), instances) => InstanceEntry(hash.toString, instances)
      case _ => unexpected("instances must point to classes")
    }.toSeq

    val repr = InterfaceMandalaModule(
      module.name,
      hadError = hasError,
      language = module.language,
      version = module.version,
      classifier = module.classifier,
      link = codeHash.map(_.toString),
      mode = module.mode,
      functions = functions,
      implements = implements,
      datatypes = datatypes,
      instances = instances,
    )
    writeToStream[InterfaceMandalaModule](repr, out, WriterConfig(indentionStep = 2))
    true
  }

  private def serializeFunClass(cls: clazz.FunClass, hasError:Boolean, out: OutputStream): Boolean = {
    assert(cls.signatures.isEmpty)
    assert(cls.implements.isEmpty)
    assert(cls.dataTypes.isEmpty)
    val functions = cls.functions.map(toFunctionRepr)
    val repr = InterfaceFunClass(
      cls.name,
      hadError = hasError,
      language = cls.language,
      version = cls.version,
      classifier = cls.classifier,
      generics = cls.classGenerics.map(toGenericRepr),
      functions = functions,
    )
    writeToStream[InterfaceFunClass](repr, out, WriterConfig(indentionStep = 2))
    true
  }

  private def serializeSigClass(cls: clazz.SigClass, codeHash:Option[Hash], hasError:Boolean, out: OutputStream): Boolean = {
    assert(cls.functions.isEmpty)
    assert(cls.implements.isEmpty)
    val signatures = cls.functions.map(toFunctionRepr)
    val datatypes = cls.dataTypes.map(toDataTypeRepr)

    val repr = InterfaceSigClass(
      cls.name,
      link = codeHash.map(_.toString),
      mode = cls.mode,
      hadError = hasError,
      language = cls.language,
      version = cls.version,
      classifier = cls.classifier,
      generics = cls.classGenerics.map(toGenericRepr),
      signatures = signatures,
      datatypes = datatypes,
      classTarget = cls.clazzLink match {
        case CompLink.ByCode(_) => unexpected("class targets should refer to classes");
        case CompLink.ByInterface(hash) => hash.toString
      },
    )
    writeToStream[InterfaceSigClass](repr, out, WriterConfig(indentionStep = 2))
    true
  }

  override def deserializeInterface(language:String, version:String, classifier:Set[String], file:InputSource, meta:Meta): Option[Interface[Component]] = {
    if(MandalaCompiler.MandalaModule_Classifier.subsetOf(classifier)) {
      deserializeMandalaModule(file,meta)
    } else if(MandalaCompiler.DefInstance_Classifier.subsetOf(classifier)) {
      deserializeInstance(file,meta)
    } else if(MandalaCompiler.FunClass_Classifier.subsetOf(classifier)) {
      deserializeFunClass(file,meta)
    } else if(MandalaCompiler.SigClass_Classifier.subsetOf(classifier)) {
      deserializeSigClass(file,meta)
    } else {
      None
    }
  }


  private def deserializeInstance(file:InputSource, meta:Meta):Option[Interface[DefInstance]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceInstance](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage("An interface was loaded that was produced by a compilation run with errors", Warning))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new DefInstanceInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new DefInstanceInterface(meta,impl))
    } catch {
      case _:Exception => None
    }
  }

  private def deserializeFunClass(file:InputSource, meta:Meta):Option[Interface[FunClass]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceFunClass](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage("An interface was loaded that was produced by a compilation run with errors", Warning))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new FunClassInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new FunClassInterface(meta,impl))
    } catch {
      case _:Exception => None
    }
  }

  private def deserializeSigClass(file:InputSource, meta:Meta):Option[Interface[SigClass]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceSigClass](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage("An interface was loaded that was produced by a compilation run with errors", Warning))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new SigClassInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new SigClassInterface(meta,impl))
    } catch {
      case _:Exception => None
    }
  }

  private def deserializeMandalaModule(file:InputSource, meta:Meta):Option[Interface[MandalaModule]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceMandalaModule](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage("An interface was loaded that was produced by a compilation run with errors", Warning))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new MandalaModuleInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new MandalaModuleInterface(meta,impl))
    } catch {
      case _:Exception => None
    }
  }

}
