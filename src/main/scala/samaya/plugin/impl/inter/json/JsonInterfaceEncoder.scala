package samaya.plugin.impl.inter.json

import java.io.OutputStream

import com.github.plokhotnyuk.jsoniter_scala.core.{WriterConfig, readFromStream, writeToStream}
import samaya.compilation.ErrorManager.{InterfaceGen, InterfaceParsing, PlainMessage, Warning, feedback, unexpected}
import samaya.plugin.impl.inter.json.JsonModel.{InterfaceModule, InterfaceTransaction}
import samaya.structure.{Component, Interface, Meta, Module, ModuleInterface, Transaction, TransactionInterface}
import samaya.plugin.service.{InterfaceEncoder, Selectors}
import samaya.structure.types.Hash
import samaya.types.InputSource

//A Interface Manager for a json description of a interface
class JsonInterfaceEncoder extends InterfaceEncoder {

  val Json = "json"

  override def matches(s: Selectors.InterfaceSelector): Boolean =  {
    def check(classifiers:Set[String]) = {
      classifiers.contains(Component.TRANSACTION_CLASSIFIER) ||
        classifiers.contains(Component.MODULE_CLASSIFIER)
    }
    s match {
      case Selectors.InterfaceDeserializationSelector(_,_,classifier,InterfaceExtension(Json)) => check(classifier)
      case Selectors.InterfaceSerializationSelector(_,_,classifier,InterfaceExtension(Json)) => check(classifier)
      case _ => false
    }
  }

  override def deserializeInterface(language:String, version:String, classifier:Set[String], file:InputSource, meta:Meta): Option[Interface[Component]] = {
    if(classifier.contains(Component.MODULE_CLASSIFIER)) {
      deserializeModuleInterface(file, meta)
    } else if(classifier.contains(Component.TRANSACTION_CLASSIFIER)) {
      deserializeTransactionInterface(file, meta)
    } else {
      None
    }
  }

  override def serializeInterface(inter:Component, codeHash:Option[Hash], hasError:Boolean, out:OutputStream): Boolean = {
    inter match {
      case module: Module => serializeModuleInterface(module, codeHash, hasError, out)
      case transaction: Transaction => serializeTransactionInterface(transaction, codeHash, hasError, out)
      case other => unexpected(s"Component $other can not be used in interface gen", InterfaceGen())
    }
  }

  //this parses a json interface, validates it and then stores it
  private def deserializeModuleInterface(file: InputSource, meta:Meta): Option[ModuleInterface] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceModule](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new ModuleInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new ModuleInterface(meta,impl))
    } catch {
      case _:Exception => None
    }
  }

  private def serializeModuleInterface(module: Module, codeHash:Option[Hash], hasError:Boolean, out: OutputStream): Boolean = {
    val repr = Serializer.toInterfaceModuleRepr(module,codeHash, hasError)
    writeToStream[InterfaceModule](repr, out, WriterConfig.withIndentionStep(2))
    true
  }

  private def deserializeTransactionInterface(file: InputSource, meta: Meta): Option[TransactionInterface] = {
    try {
      //Parse the input to an AST using Interface as parsing description
      val interfaceAst = readFromStream[InterfaceTransaction](file.content)
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file.identifier.fullName, interfaceAst.name)
      val impl = new TransactionInterfaceImpl(baseLoc, interfaceAst)
      //return the module on success
      Some(new TransactionInterface(meta,impl))
    } catch {
      case _:Exception => None
    }
  }

  private def serializeTransactionInterface(transaction: Transaction, codeHash:Option[Hash], hasError:Boolean, out: OutputStream): Boolean = {
    val repr = Serializer.toInterfaceTransactionRepr(transaction,codeHash,hasError)
    writeToStream[InterfaceTransaction](repr, out, WriterConfig.withIndentionStep(2))
    true
  }
}
