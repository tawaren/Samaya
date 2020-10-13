package samaya.toolbox.process

import samaya.structure.types.OpCode.{VirtualOpcode, ZeroSrcOpcodes}
import samaya.structure.types.{SourceId, _}
import samaya.structure.{Param, _}
import samaya.toolbox.process.TypeInference.{TypeInference, TypeReplacing}
import samaya.toolbox.track.TypeTracker
import samaya.toolbox.transform.{ComponentTransformer, TransformTraverser}
import samaya.types.Context

import scala.collection.immutable.ListMap

object CaseSorter extends ComponentTransformer {

  case class TypeHint(src: Ref, typ: Type, id: SourceId) extends VirtualOpcode with ZeroSrcOpcodes

  override def transformFunction(in: FunctionDef, context: Context): FunctionDef = {
    val transformer = new CaseSorter(Left(in), context)
    new FunctionDef {
      override val src: SourceId = in.src
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
    val transformer = new CaseSorter(Right(in), context)
    new ImplementDef {
      override val src: SourceId = in.src
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


  class CaseSorter(override val component: Either[FunctionDef, ImplementDef], override val context: Context) extends TransformTraverser with TypeTracker {
    override def transformSwitch(res: Seq[AttrId], src: Ref, bodies: ListMap[Id, (Seq[AttrId], Seq[OpCode])], mode: FetchMode, origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val branches = stack.getType(src) match {
        case adt:AdtType =>
          var builder = ListMap.newBuilder[Id,  (Seq[AttrId], Seq[OpCode])]
          for((key,_) <- adt.ctrs(context); idKey = Id(key); value <- bodies.get(Id(key))) {
            builder += ((idKey, value))
          }
          builder.result()
        case _ => bodies
      }
      Some(List(OpCode.Switch(res,src,branches,mode, origin)))
    }
    override def transformInspect(res: Seq[AttrId], src: Ref, bodies: ListMap[Id, (Seq[AttrId], Seq[OpCode])], origin: SourceId, stack: Stack): Option[Seq[OpCode]] = {
      val branches = stack.getType(src) match {
        case adt:AdtType =>
          var builder = ListMap.newBuilder[Id,  (Seq[AttrId], Seq[OpCode])]
          for((key,_) <- adt.ctrs(context); idKey = Id(key); value <- bodies.get(idKey)) {
            builder += ((idKey, value))
          }
          builder.result()
        case _ => bodies
      }
      Some(List(OpCode.Inspect(res,src,branches, origin)))
    }
  }
}