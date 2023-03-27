package samaya.plugin.service

import samaya.plugin.{Plugin, PluginProxy}
import samaya.plugin.service.ReferenceResolver.ReferenceType
import samaya.plugin.service.category.ReferenceResolverPluginCategory
import samaya.types.{Address, GeneralSource}

import scala.reflect.ClassTag


//a plugin description for managing (parsing and validating) interface descriptions
trait ReferenceResolver extends Plugin {
  override type Selector = Selectors.ReferenceResolverSelector
  def resolveAll(source:GeneralSource, filter:Option[Set[ReferenceType]]):Map[ReferenceType,Seq[Address]]
  def resolve(source:GeneralSource, typ:ReferenceType):Seq[Address]
}


object ReferenceResolver extends ReferenceResolver with PluginProxy{

  sealed trait ReferenceType
  case object Repository extends ReferenceType
  case object Package extends ReferenceType
  case object Workspace extends ReferenceType
  case object Source extends ReferenceType

  type PluginType = ReferenceResolver
  override def classTag: ClassTag[PluginType] = implicitly[ClassTag[PluginType]]
  override def category: PluginCategory[PluginType] = ReferenceResolverPluginCategory

  //Todo: can we lift the silent out of this - see uses
  override def resolveAll(source:GeneralSource, filter: Option[Set[ReferenceType]] = None): Map[ReferenceType, Seq[Address]] = {
    PluginProxy.resolveSilent(selectAll(Selectors.ResolveAllReferencesSelector(source, filter))).flatMap(_.resolveAll(source,filter)).groupMap(_._1)(_._2).view.mapValues(_.flatten).toMap
  }

  //Todo: can we lift the silent out of this - see uses
  override def resolve(source:GeneralSource, typ: ReferenceType): Seq[Address] = {
    PluginProxy.resolveSilent(selectAll(Selectors.ResolveSingleReferencesSelector(source,typ))).flatMap(_.resolve(source,typ))
  }
}


