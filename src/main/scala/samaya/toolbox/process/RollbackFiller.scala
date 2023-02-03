package samaya.toolbox.process

import samaya.structure.types.OpCode.{VirtualOpcode, ZeroSrcOpcodes}
import samaya.structure.types.{SourceId, _}
import samaya.structure.{Param, _}
import samaya.toolbox.track.OwnershipTracker.Owned
import samaya.toolbox.track.{OwnershipTracker, TypeTracker}
import samaya.toolbox.transform.{EntryTransformer, TransformTraverser}
import samaya.types.Context

import scala.collection.immutable.ListMap

object RollbackFiller extends EntryTransformer {

  override def transformFunction(in: FunctionDef, context: Context): FunctionDef = {
    val transformer = new RollbackFiller(Left(in), context)
    new FunctionDef {
      override val src:SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = in.generics
      override val params: Seq[Param] = in.params
      override val results: Seq[Result] = in.results
      override val transactional: Boolean = in.transactional
      override val position: Int = in.position
    }
  }

  override def transformImplement(in: ImplementDef, context: Context): ImplementDef = {
    val transformer = new RollbackFiller(Right(in), context)
    new ImplementDef {
      override val src:SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = in.generics
      override val params: Seq[Param] = in.params
      override val results: Seq[Result] = in.results
      override val position: Int = in.position
      override val sigParamBindings: Seq[Binding] = in.sigParamBindings
      override val sigResultBindings: Seq[Binding] = in.sigResultBindings
      override val transactional: Boolean = in.transactional
    }
  }


  class RollbackFiller(override val entry: Either[FunctionDef, ImplementDef], override val context: Context) extends TransformTraverser with OwnershipTracker {
    override def transformRollback(rets: Seq[AttrId], params: Seq[Ref], types: Seq[Type], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      if(params.isEmpty) {
        val consumes = stack.slots.keys.filter(v => stack.getStatus(v) == Owned && !stack.getType(v).hasCap(context, Capability.Drop))
        //We need to sort to make compilation result deterministic as stack.slots.keys is not guaranteed to be a deterministic iterator
        val sorted_consumes = consumes.toSeq.sortWith(_.name <= _.name)
        Some(Seq(OpCode.RollBack(rets,sorted_consumes,types,origin)))
      } else {
        None
      }
    }
  }
}