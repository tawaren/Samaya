package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Compiler, Error, LocatedMessage, feedback}
import samaya.plugin.impl.compiler.mandala.MandalaParser
import samaya.structure.types.Capability

trait CapabilityCompiler extends CompilerToolbox {

  val dataCapsDefault:Set[Capability] = Set(Capability.Value, Capability.Persist, Capability.Unbound, Capability.Copy, Capability.Drop)
  val sigCapsDefault:Set[Capability] = Set(Capability.Drop)
  val genericDataCapsDefault:Set[Capability] = Set.empty
  val genericFunCapsDefault:Set[Capability] = Set(Capability.Value, Capability.Unbound)

  private var defaultCaps:Set[Capability] = dataCapsDefault

  def withDefaultCaps[T](defs:Set[Capability])(body: => T): T = {
    val old = defaultCaps
    defaultCaps = defs
    val res = body
    defaultCaps = old
    res
  }

  override def visitCapabilities(ctx: MandalaParser.CapabilitiesContext): Set[Capability] = {
    val sourceId = sourceIdFromContext(ctx);

    def unusedCheck[T](list:java.util.List[T]): Unit = {
      if (list != null && !list.isEmpty) {
        feedback(LocatedMessage("no other capability classes allowed if the primitive modifier is used", sourceId, Error, Compiler()))
      }
    }

    //TODO: issue a warning if an Option does not change the default
    if(ctx.PRIMITIVE() != null  && !ctx.PRIMITIVE().isEmpty) {
      if(ctx.PRIMITIVE().size() != 1) {
        feedback(LocatedMessage("can only have one capability class related to primitivity", sourceId,Error, Compiler()))
      } else {
        //todo: ensure no other mods
        unusedCheck(ctx.volatility())
        unusedCheck(ctx.scoped())
        unusedCheck(ctx.persistancy())
        unusedCheck(ctx.substructural())
        return Capability.all
      }
    }
    var caps:Set[Capability] = defaultCaps
    if(ctx.volatility() != null && !ctx.volatility().isEmpty) {
      if(ctx.volatility().size() != 1) {
        feedback(LocatedMessage("can only have one capability class related to volatility", sourceId,Error, Compiler()))
      } else {
        if(ctx.volatility().get(0).VOITAILE() != null) caps = caps - Capability.Value
        if(ctx.volatility().get(0).VALUE() != null) caps = caps + Capability.Value
      }

    }

    if(ctx.persistancy() != null && !ctx.persistancy().isEmpty) {
      if(ctx.persistancy().size() != 1) {
        feedback(LocatedMessage("can only have one capability class related to persistance", sourceId,Error, Compiler()))
      } else {
        if(ctx.persistancy().get(0).TEMPORARY() != null) caps = caps - Capability.Persist
        if(ctx.persistancy().get(0).PERSISTED() != null) caps = caps + Capability.Persist
      }
    }

    if(ctx.scoped() != null && !ctx.scoped().isEmpty) {
      if(ctx.scoped().size() != 1) {
        feedback(LocatedMessage("can only have one capability class related to scope", sourceId,Error, Compiler()))
      } else {
        if(ctx.scoped().get(0).BOUNDED() != null) caps = caps - Capability.Unbound
        if(ctx.scoped().get(0).UNBOUNDED() != null) caps = caps + Capability.Unbound
      }
    }

    if(ctx.substructural() != null  && !ctx.substructural().isEmpty) {
      if(ctx.substructural().size() != 1) {
        feedback(LocatedMessage("can only have one capability class related to substructure", sourceId,Error, Compiler()))
      } else {
        if(ctx.substructural().get(0).STANDARD() != null) caps = caps + Capability.Copy + Capability.Drop
        if(ctx.substructural().get(0).AFFINE() != null) caps = caps + Capability.Drop - Capability.Copy
        if(ctx.substructural().get(0).RELEVANT() != null) caps = caps + Capability.Copy - Capability.Drop
        if(ctx.substructural().get(0).LINEAR() != null) caps = caps - Capability.Copy - Capability.Drop
      }
    }

    caps
  }

}
