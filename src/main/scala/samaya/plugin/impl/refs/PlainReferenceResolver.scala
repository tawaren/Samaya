package samaya.plugin.impl.refs

import samaya.plugin.service.{AddressResolver, ReferenceResolver, Selectors}
import samaya.types._

import scala.io.Source

//A Dependency Manager for a json description of a dependency list
trait PlainReferenceResolver extends ReferenceResolver {

  protected val ext:String
  protected val TYP:ReferenceResolver.ReferenceType
  override def matches(s: Selectors.ReferenceResolverSelector): Boolean = {
    s match {
      case Selectors.ResolveAllReferencesSelector(input : InputSource, None) => input.identifier.extension.contains(ext)
      case Selectors.ResolveAllReferencesSelector(input : InputSource, Some(filter)) => filter.contains(TYP) && input.identifier.extension.contains(ext)
      case Selectors.ResolveSingleReferencesSelector(input : InputSource, TYP) => input.identifier.extension.contains(ext)
      case _ => false
    }
  }

  def resolveElems(source: GeneralSource): Seq[Address] = {
    val file = source match {
      case source: InputSource => source
      case _ => return Seq.empty
    }
    val addresses = file.read{r =>
      Source.fromInputStream(r).getLines()
    }
    addresses.flatMap(AddressResolver.parsePath).toSeq
  }

  override def resolveAll(source: GeneralSource, filter: Option[Set[ReferenceResolver.ReferenceType]]): Map[ReferenceResolver.ReferenceType, Seq[Address]] = {
    filter match {
      case Some(fs) if fs.contains(ReferenceResolver.Repository) => Map(TYP -> resolveElems(source))
      case None => Map(TYP -> resolveElems(source))
      case _ => Map.empty
    }
  }

  override def resolve(source: GeneralSource, typ: ReferenceResolver.ReferenceType): Seq[Address] = {
    typ match {
      case TYP => resolveElems(source)
      case _ => Seq.empty
    }
  }
}
