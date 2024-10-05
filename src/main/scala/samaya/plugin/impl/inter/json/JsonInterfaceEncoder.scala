package samaya.plugin.impl.inter.json

import java.io.OutputStream
import com.github.plokhotnyuk.jsoniter_scala.core.{WriterConfig, readFromStream, writeToStream}
import samaya.compilation.ErrorManager.{InterfaceGen, InterfaceParsing, PlainMessage, Warning, feedback, unexpected}
import samaya.config.ConfigValue
import samaya.plugin.config.ConfigPluginCompanion
import samaya.plugin.impl.inter.json.JsonModel.{InterfaceModule, InterfaceTransaction}
import samaya.structure.{Component, Interface, Meta, Module, ModuleInterface, Transaction, TransactionInterface}
import samaya.plugin.service.{InterfaceEncoder, Selectors}
import samaya.structure.types.Hash
import samaya.types.InputSource


object JsonInterfaceEncoder extends ConfigPluginCompanion {
  val Json:String = "json"
  val format: ConfigValue[String] = arg("interface.encoder.format|encoder.format|format").default(Json)
}

import samaya.plugin.impl.inter.json.JsonInterfaceEncoder._

//A Interface Manager for a json description of a interface
class JsonInterfaceEncoder extends InterfaceEncoder {

  override def matches(s: Selectors.InterfaceSelector): Boolean =  {
    def check(classifiers:Set[String]) = {
      classifiers.contains(Component.TRANSACTION.classifier) ||
        classifiers.contains(Component.MODULE.classifier)
    }
    s match {
      case Selectors.InterfaceDecoderSelector(_,_,classifier,InterfaceExtension(Json)) => check(classifier)
      case Selectors.InterfaceEncoderSelector(_,_,classifier,InterfaceExtension(Json)) if format.value == Json => check(classifier)
      case Selectors.InterfaceFormatSelector => true
      case _ => false
    }
  }

  override def deserializeInterface(language:String, version:String, classifier:Set[String], file:InputSource, meta:Meta): Option[Interface[Component]] = {
    if(classifier.contains(Component.MODULE.classifier)) {
      deserializeModuleInterface(file, meta)
    } else if(classifier.contains(Component.TRANSACTION.classifier)) {
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
      val interfaceAst = file.read(readFromStream[InterfaceModule](_))
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file, interfaceAst.name)
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
      val interfaceAst = file.read(readFromStream[InterfaceTransaction](_))
      if(interfaceAst.hadError) feedback(PlainMessage(s"The interface ${file.identifier.fullName} was produced by a compilation run with errors", Warning, InterfaceParsing()))
      //convert the AST to the the internal shared representation of Modules
      val baseLoc = JsonLocation(file, interfaceAst.name)
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

  override def defaultFormat(): Option[String] = Some(Json)
}