package samaya.plugin.impl.compiler.mandala.process

import samaya.compilation.ErrorManager._
import samaya.plugin.impl.compiler.mandala.components.InstInfo
import samaya.plugin.impl.compiler.mandala.components.clazz.Class
import samaya.plugin.impl.compiler.mandala.components.instance.Instance.EntryRef
import samaya.structure.types.{Accessibility, AttrId, CompLink, DefinedFunc, Func, Id, ImplFunc, OpCode, Permission, Ref, SourceId, StdFunc, Type}
import samaya.structure.{Attribute, Binding, FunctionDef, Generic, ImplementDef, Interface, Module, Param, Result}
import samaya.toolbox.process.TypeInference
import samaya.toolbox.process.TypeInference.TypeVar
import samaya.toolbox.track.JoinType
import samaya.toolbox.transform.ComponentTransformer
import samaya.types.Context

class TypeAndClassInference(instanceFinder:InstanceFinder)  extends ComponentTransformer{

  override def transformFunction(in: FunctionDef, context: Context): FunctionDef = {
    val analyzer = new TypeAndClassInference(Left(in), context)
    val result = analyzer.extract()
    val transformer = new TypeAndClassReplacing(result, Left(in), context)
    val adapted = transformer.component.left.get
    new FunctionDef {
      override val src: SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = adapted.generics
      override val params: Seq[Param] = adapted.params
      override val results: Seq[Result] = adapted.results
      override val transactional: Boolean = in.transactional
      override val position: Int = in.position
    }
  }

  override def transformImplement(in: ImplementDef, context: Context): ImplementDef = {
    val analyzer = new TypeAndClassInference(Right(in), context)
    val result = analyzer.extract()
    val transformer = new TypeAndClassReplacing(result, Right(in), context)
    val adapted = transformer.component.right.get
    new ImplementDef {
      override val src: SourceId = in.src
      override val code: Seq[OpCode] = transformer.transform()
      override val external: Boolean = in.external
      override val index: Int = in.index
      override val name: String = in.name
      override val attributes: Seq[Attribute] = in.attributes
      override val accessibility: Map[Permission, Accessibility] = in.accessibility
      override val generics: Seq[Generic] = adapted.generics
      override val params: Seq[Param] = adapted.params
      override val results: Seq[Result] = adapted.results
      override val position: Int = in.position
      override val sigParamBindings: Seq[Binding] = adapted.sigParamBindings
      override val sigResultBindings: Seq[Binding] = adapted.sigResultBindings
      override val transactional: Boolean = in.transactional
    }
  }

  case class ClassCallInfo(
    name:String,
    comp:CompLink,
    classApplies:Seq[Type],
    src:SourceId,
  )

  //Makes it extendable
  trait InferenceData extends TypeInference.InferenceData {
    def funSubstitutions: Map[Func, Func]
  }

  case class AnalysisResults(
    override val substitutions: Map[TypeVar, Type],
    override val joins: Map[JoinType, Type],
    override val funSubstitutions:Map[Func, Func]
  ) extends InferenceData

  class TypeAndClassInference(component: Either[FunctionDef, ImplementDef], context: Context) extends TypeInference.TypeInference(component,context)  {

    var foundClasses:Map[Func, ClassCallInfo] = Map.empty

    def findAndRecordClassFunc(func:Func, src: SourceId): Unit ={
      func match {
        case func: DefinedFunc[_] =>
          func.getComponent(context) match {
            //This is save as funcs can not be ocal
            // If we are in a class then we have no bodies and thus never end up here
            // If we are not in a class then the func always points to a remote (where we already have the interface)
            case Some(cls:Class with Interface[_]) =>
              val appls = func.applies.take(cls.classGenerics.size)
              foundClasses = foundClasses.updated(func, ClassCallInfo(func.name(context),cls.link, appls, src))
            case _ =>

          }
        case _ =>
      }
    }

    override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
      findAndRecordClassFunc(func, origin)
      super.invoke(res, func, params, origin, stack)
    }

    override def tryInvokeBefore(res: Seq[AttrId], func: Func, params: Seq[(Boolean, Ref)], succ: (Seq[AttrId], Seq[OpCode]), fail: (Seq[AttrId], Seq[OpCode]), origin: SourceId, stack: Stack): Stack = {
      findAndRecordClassFunc(func, origin)
      super.tryInvokeBefore(res, func, params, succ, fail, origin, stack)
    }

    override def extract(): InferenceData = {
      val parent = super.extract()
      val subs = parent.substitutions
      var funSubs:Map[Func,Func] = Map.empty
      //repeat as long as we make progress
      for((func, ClassCallInfo(name, comp, applies, src)) <- foundClasses) {
        val resolvedApplies = applies.map {
          case t:TypeVar => subs(t)
          case t => t
        }
        instanceFinder.findAndApplyTargetFunction(name, comp, resolvedApplies, func.applies, context, src) match {
          case Some(newFunc) => funSubs = funSubs + (func -> newFunc)
            //Error was printed in findAndApplyTargetFunction
          case None => funSubs = funSubs + (func -> Func.Unknown)
        }
      }
      AnalysisResults(parent.substitutions, parent.joins, funSubs)
    }
  }

  class TypeAndClassReplacing(lookup: InferenceData, prevComp: Either[FunctionDef, ImplementDef], context: Context) extends TypeInference.TypeReplacing(lookup, prevComp, context){
    override protected def substituteFunc(f: Func): Func = {
      super.substituteFunc(lookup.funSubstitutions.getOrElse(f,f))
    }
  }

}
