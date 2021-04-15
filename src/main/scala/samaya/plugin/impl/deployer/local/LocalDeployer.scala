package samaya.plugin.impl.deployer.local

import java.io.{ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.net.Socket
import java.nio.charset.StandardCharsets

import samaya.compilation.ErrorManager._
import samaya.deploy.Deployer
import samaya.plugin.service.Selectors
import samaya.structure.{Interface, Transaction, Module}
import samaya.structure.types.Hash


class LocalDeployer extends Deployer{

  val MODULE_COMMAND:Byte = 0
  val TRANSACTION_COMMAND:Byte = 1
  val SYS_MODULE_COMMAND:Byte = 2

  val SUCCESS_RETURN:Byte = 0
  val ERROR_RETURN:Byte = 1

  override def matches(s: Selectors.DeployerSelector): Boolean = true

  def sendOverSocket(typ:Byte, meta:Array[Byte], sysId:Option[Short], data:Array[Byte]): Option[Hash] = {
    val socket = new Socket("localhost", 6000)
    try {
      val out = new DataOutputStream(socket.getOutputStream)
      out.writeByte(typ)
      out.writeInt(meta.length)
      out.write(meta,0,meta.length)
      if(sysId.isDefined) {
        val sysIdValue = sysId.get
        val hasId = sysIdValue >= 0 && sysIdValue <= Byte.MaxValue.asInstanceOf[Short]
        if(hasId) {
          out.writeByte(1)
          out.writeByte(sysIdValue.asInstanceOf[Byte])
        } else {
          out.writeByte(0)
        }
      }
      out.writeInt(data.length)
      out.write(data,0,data.length)
      val in = new DataInputStream(socket.getInputStream)
      in.readByte() match {
        case ERROR_RETURN => None
        case SUCCESS_RETURN => Some(Hash(in.readNBytes(20)));
        case _ => None
      }
    } catch {
      case e:Exception =>
        feedback(PlainMessage(s"deploy error: ${e.getMessage}", Error, Always))
        None
    } finally {
      socket.close()
    }
  }


  private def generateMetaData(module:Interface[_] with Module):Array[Byte] = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val out = new DataOutputStream(byteArrayOutputStream)
    def writeString(str:String) = {
      val strBytes = str.getBytes(StandardCharsets.UTF_8)
      out.writeShort(strBytes.size.asInstanceOf[Short])
      out.write(strBytes,0,strBytes.length)
    }
    writeString(module.name)
    out.writeByte(module.dataTypes.size.asInstanceOf[Byte])
    for(dataType <- module.dataTypes) {
      writeString(dataType.name)
      out.writeByte(dataType.constructors.size.asInstanceOf[Byte])
      for(ctr <- dataType.constructors) {
        writeString(ctr.name)
        out.writeByte(ctr.fields.size.asInstanceOf[Byte])
      }
    }
    out.flush()
    byteArrayOutputStream.toByteArray
  }

  override def deployModule(module:Interface[_] with Module): Option[Hash] = {
    module.mode match {
      case Module.Precompile(id) => sendOverSocket(SYS_MODULE_COMMAND, generateMetaData(module), Some(id.asInstanceOf[Short]), module.meta.code.get.content.readAllBytes())
      case Module.Elevated => sendOverSocket(SYS_MODULE_COMMAND, generateMetaData(module), Some(-1), module.meta.code.get.content.readAllBytes())
      case Module.Normal => sendOverSocket(MODULE_COMMAND, generateMetaData(module), None, module.meta.code.get.content.readAllBytes())
    }
  }

  //Todo: Capture and return the Descriptor
  override def deployTransaction(txt:Interface[_] with Transaction): Option[Hash] = {
    val nameBytes =  txt.name.getBytes(StandardCharsets.UTF_8)
    sendOverSocket(TRANSACTION_COMMAND, nameBytes, None, txt.meta.code.get.content.readAllBytes())
  }

}
