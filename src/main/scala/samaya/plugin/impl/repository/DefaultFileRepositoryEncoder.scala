package samaya.plugin.impl.repository

import samaya.compilation.ErrorManager.{CodeGen, PlainMessage, Warning, feedback}
import samaya.config.ConfigValue
import samaya.plugin.config.ConfigPluginCompanion
import samaya.plugin.service.{AddressResolver, ContentRepositoryEncoder, Selectors}
import samaya.types.Address.HybridAddress
import samaya.types.Repository.AddressableRepository
import samaya.types.{Address, Directory, GeneralSink, GeneralSource, Identifier, InputSource, OutputTarget, RepositoryBuilder}

import scala.io.Source
import samaya.plugin.service.AddressResolver.{DynamicLocation, Hybrid}

import java.io.PrintWriter
import scala.util.Using

object DefaultFileRepositoryEncoder extends ConfigPluginCompanion {
  val Repo:String = "repo"
  val defaultRepositoryName:Identifier.Specific = Identifier.Specific("",Repo)
  val format: ConfigValue[String] = arg("repository.encoder.format|encoder.format|format").default(Repo)
}

import samaya.plugin.impl.repository.DefaultFileRepositoryEncoder._

class DefaultFileRepositoryEncoder extends ContentRepositoryEncoder{

  override def matches(s: Selectors.RepositoryEncoderSelector): Boolean = s match {
    case Selectors.LoadRepository(source : InputSource) => source.identifier == defaultRepositoryName
    case Selectors.LoadRepository(dir : Directory) =>  AddressResolver.resolve(dir.resolveAddress(Address(defaultRepositoryName)), AddressResolver.InputLoader).isDefined
    case Selectors.CreateRepository(_ : Directory) if format.value == Repo =>  true
    case Selectors.CreateRepository(sink : OutputTarget) if format.value == Repo => sink.identifier == defaultRepositoryName
    case _ => false
  }

  override def loadRepository(generalSource:GeneralSource): Option[AddressableRepository] = {
    val source = generalSource match {
      case source: InputSource => source
      case dir: Directory =>
        val addr = dir.resolveAddress(Address(defaultRepositoryName))
        AddressResolver.resolve(addr, AddressResolver.InputLoader) match {
          case Some(source) => source
          case None => return None;
        }
      case _ => return None;
    }

    val root = source.location
    val repo = source.read{ in =>
      Source.fromInputStream(in).getLines().flatMap(AddressResolver.parsePath).flatMap{
        case HybridAddress(target, loc) => Some((target.target, loc))
        case _ => None //Todo: Error <-- also an error in the previous flat map
      }.toMap
    }
    Some(new MapIndexedRepository(root, source.identifier, repo))
  }


  override def storeRepository(sink: GeneralSink, repository:RepositoryBuilder): Boolean = {
    val (directory, index) = sink match {
      case dir: Directory => AddressResolver.resolveSink(dir, defaultRepositoryName) match {
        case Some(src) => (dir, src)
        case None => return false
      }
      case snk: OutputTarget => (snk.location, snk)
      case _ => return false
    }
    index.write( out => {
      Using(new PrintWriter(out)) { writer =>
        repository.result().values.foreach { content =>
          AddressResolver.serializeContentAddress(content, Hybrid(DynamicLocation(directory))) match {
            case Some(address) => writer.println(address)
            case None => feedback(PlainMessage("", Warning, CodeGen()))
          }
        }
      }
    })
    true
  }
}
