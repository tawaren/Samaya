package samaya.plugin.impl.compiler.mandala.inter.json

import java.io.OutputStream
import com.github.plokhotnyuk.jsoniter_scala.core.{WriterConfig, readFromStream, writeToStream}
import samaya.compilation.ErrorManager.{ExceptionError, InterfaceGen, InterfaceParsing, PlainMessage, Warning, feedback, unexpected}
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.clazz
import JsonModel.{Applied, Implement, InstanceEntry, InterfaceFunClass, InterfaceInstance, InterfaceMandalaModule, InterfaceSigClass, TypeAlias}
import samaya.plugin.impl.compiler.mandala.components.clazz.{FunClass, FunClassInterface, SigClass, SigClassInterface}
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, DefInstanceInterface}
import samaya.plugin.impl.compiler.mandala.components.module.{MandalaModule, MandalaModuleInterface}
import samaya.plugin.impl.compiler.mandala.entry
import samaya.plugin.impl.compiler.mandala.entry.SigImplement
import samaya.plugin.impl.inter.json.Serializer.{toDataTypeRepr, toFunctionRepr, toGenericRepr, toTypeRepr}
import samaya.plugin.impl.inter.json.{JsonLocation, Serializer}
import samaya.plugin.service.{InterfaceEncoder, Selectors}
import samaya.structure.types.{AdtType, CompLink, DataType, Hash, ImplFunc, LitType, StdFunc}
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
      case other => unexpected(s"Component $other can not be used in interface gen", InterfaceGen())
    }
  }


  private def serializeInstanceInterface(inst: DefInstance, hasError:Boolean, out: OutputStream): Boolean = {
    val generics = inst.generics.map(toGenericRepr)
    val repr = InterfaceInstance(
      name = inst.name,
      hadError = hasError,
      language = inst.language,
      version = inst.version,
      classifier = inst.classifier,
      generics = generics,
      classTarget = inst.classTarget match {
        case CompLink.ByCode(_) => unexpected("Class targets should refer to classes", InterfaceGen());
        case CompLink.ByInterface(hash) => hash.toString
      },
      applies = inst.classApplies.map(Serializer.toTypeRepr),
      implements = inst.implements.map{
        case SigImplement(name, generics, funTarget, implTarget, _) =>
          val fun = funTarget match {
            case StdFunc.Remote(module, offset, applies) => Applied(Some(module.toString), offset, applies.map(toTypeRepr))
            case StdFunc.Local(offset, applies) => Applied(None, offset, applies.map(toTypeRepr))
          }
          val impl = implTarget match {
            case ImplFunc.Remote(module, offset, applies) => Applied(Some(module.toString), offset, applies.map(toTypeRepr))
            case ImplFunc.Local(offset, applies) => Applied(None, offset, applies.map(toTypeRepr))
          }
          Implement(name,generics.map(toGenericRepr),fun,impl)
      }
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
      case _ => unexpected("instances must point to classes", InterfaceGen())
    }.toSeq

    val typeAlias = module.typeAlias.map{
      case entry.TypeAlias(name, gens, target, _ ) =>
        TypeAlias(name, gens.map(toGenericRepr), toTypeRepr(target))
    }

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
      typeAlias = typeAlias,
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
      generics = cls.generics.map(toGenericRepr),
      functions = functions,
    )
    writeToStream[InterfaceFunClass](repr, out, WriterConfig(indentionStep = 2))
    true
  }

  private def serializeSigClass(cls: clazz.SigClass, codeHash:Option[Hash], hasError:Boolean, out: OutputStream): Boolean = {
    assert(cls.functions.isEmpty)
    assert(cls.implements.isEmpty)
    val signatures = cls.signatures.map(toFunctionRepr)
    val datatypes = cls.dataTypes.map(toDataTypeRepr)

    val repr = InterfaceSigClass(
      cls.name,
      link = codeHash.map(_.toString),
      mode = cls.mode,
      hadError = hasError,
      language = cls.language,
      version = cls.version,
      classifier = cls.classifier,
      generics = cls.generics.map(toGenericRepr),
      signatures = signatures,
      datatypes = datatypes,
      //Todo: Why does this trigger -- do we deserialize wrongly somwhere??
      classTarget = cls.clazzLink match {
        case CompLink.ByCode(_) =>
          println(cls.clazzLink)
          unexpected("class targets should refer to classes", InterfaceGen());
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
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new DefInstanceInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new DefInstanceInterface(meta,impl))
    } catch {
      case e:Exception =>
        e.printStackTrace()
        feedback(ExceptionError(e))
        None
    }
  }

  private def deserializeFunClass(file:InputSource, meta:Meta):Option[Interface[FunClass]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceFunClass](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new FunClassInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new FunClassInterface(meta,impl))
    } catch {
      case e:Exception =>
        feedback(ExceptionError(e))
        None
    }
  }

  private def deserializeSigClass(file:InputSource, meta:Meta):Option[Interface[SigClass]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceSigClass](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new SigClassInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new SigClassInterface(meta,impl))
    } catch {
      case e:Exception =>
        feedback(ExceptionError(e))
        None
    }
  }

  private def deserializeMandalaModule(file:InputSource, meta:Meta):Option[Interface[MandalaModule]] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceMandalaModule](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new MandalaModuleInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new MandalaModuleInterface(meta,impl))
    } catch {
      case e:Exception =>
        feedback(ExceptionError(e))
        None
    }
  }

}
