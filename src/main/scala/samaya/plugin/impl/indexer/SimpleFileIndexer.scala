package samaya.plugin.impl.indexer

import samaya.build.BuildRepository
import samaya.compilation.ErrorManager
import samaya.compilation.ErrorManager.{CodeGen, PlainMessage, Warning, feedback}
import samaya.plugin.service.AddressResolver.Hybrid
import samaya.plugin.service.{AddressResolver, ContentLocationIndexer, Selectors}
import samaya.types.{ContentAddressable, Directory, Identifier}

import java.io.PrintWriter



class SimpleFileIndexer extends ContentLocationIndexer{
  private val sink = Identifier("index","repo")

  override def matches(s: Selectors.ContentSelector): Boolean = s match {
    case Selectors.UpdateContentIndex => false
    case Selectors.StoreContentIndex(directory) => AddressResolver.resolveSink(directory, sink).isDefined
  }

  override def indexContent(content: ContentAddressable): Boolean = false

  override def storeIndex(directory: Directory): Boolean = {
    AddressResolver.resolveSink(directory, sink) match {
      case Some(index) => index.write( out => {
        val writer = new PrintWriter(out)
        BuildRepository.repo.values.foreach { content =>
            AddressResolver.serializeAddress(Some(directory), content, Hybrid) match {
              case Some(address) => writer.println(address)
              case None => feedback(PlainMessage("", Warning, CodeGen()))
            }
        }
      })
      case None => return false
    }
    true
  }
}
