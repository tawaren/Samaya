package samaya.toolbox.track

import samaya.structure.types.Type.{Projected, Unknown}
import samaya.structure.types._
import samaya.toolbox.stack.SlotFrameStack.SlotDomain
import samaya.structure.{Attribute, Param}

/**
  * The Type Tracker associates a type with each value produced by the Value Tracker
  *  It corresponds to a basic type inferencer
  *   If it can not infer the Type it will produce the Type.DefaultUnknown type
  *  others can read the type over Stack.getType(...)
  */
trait TypeTracker extends ValueTracker {

  //provide an implicit view of the stack that knows how to fetch / set types
  final implicit class TypedStack(s:Stack) {
    def getType(id:Ref):Type = s.readSlot(TypeTracker,id).map(_.changeMeta()).getOrElse(Type.Unknown(Set.empty)(id.src))
    private[TypeTracker] def withType(id:Ref, t:Type):Stack = s.updateSlot(TypeTracker,id)(_ => Some(t))

  }

  override def abstractedBody(rets: Seq[Id], src: SourceId, stack: Stack): Stack = {
    val nStack = super.abstractedBody(rets, src, stack)
    rets.zip(results).foldLeft(nStack){
      case (s, (id, res))  => s.withType(id,res.typ)
    }
  }

  override def functionStart(params:Seq[Param], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.functionStart(params, origin, stack)
    params.foldLeft(nStack)((s, p) => s.withType(getParamVal(p),p.typ))
  }

  override def caseStart(fields: Seq[AttrId], src: Ref, ctr: Id, mode: Option[FetchMode], origin: SourceId, stack: Stack): Stack ={
    val nType = stack.getType(stack.resolve(src))
    val argTypes = nType.projectionSeqMap{
      case adt:AdtType => adt.ctrs(context).get(ctr.name).map(_.values.toSeq).getOrElse(Seq.empty).padTo(fields.size, Type.Unknown(Set.empty)(origin))
      case _ => Seq.fill(fields.size)(Type.Unknown(Set.empty)(origin))
    }

    val nStack = super.caseStart(fields, src, ctr, mode, origin, stack)
    fields.zip(argTypes).foldLeft(nStack){
      case (s, (id,cType)) => s.withType(id,cType)
    }
  }

  override def invokeSuccStart(fields: Seq[AttrId], call: Either[Func, Ref], origin: SourceId, stack: Stack): Stack = {
    val argTypes = call match {
      case Left(func) => func.returnInfo(context).padTo(fields.size, Type.Unknown(Set.empty)(origin))
      case Right(value) => stack.getType(value) match {
        case sdt:SigType => sdt.returnInfo(context).padTo(fields.size, Type.Unknown(Set.empty)(origin))
        case _ => Seq.fill(fields.size)(Type.Unknown(Set.empty)(origin))
      }
    }

    val nStack = super.invokeSuccStart(fields, call, origin, stack)
    fields.zip(argTypes).foldLeft(nStack){
      case (s, (id,cType)) => s.withType(id,cType)
    }
  }

  override def invokeFailStart(fields: Seq[AttrId], call: Either[Func, Ref], essential: Seq[Boolean], origin: SourceId, stack: Stack): Stack = {
    val argTypes = (call match {
      case Left(func) => func.paramInfo(context).map(_._1).padTo(essential.size, Type.Unknown(Set.empty)(origin))
      case Right(value) => stack.getType(value) match {
        case sdt:SigType => sdt.paramInfo(context).map(_._1).padTo(essential.size, Type.Unknown(Set.empty)(origin))
        case _ => Seq.fill(essential.size)(Type.Unknown(Set.empty)(origin))
      }
    }).zip(essential).filter(_._2).map(_._1).padTo(fields.size, Type.Unknown(Set.empty)(origin))
    val nStack = super.invokeFailStart(fields, call, essential, origin, stack)
    fields.zip(argTypes).foldLeft(nStack){
      case (s, (id,cType)) => s.withType(id,cType)
    }
  }

  override def lit(res:TypedId, value:Const, origin:SourceId, stack: Stack): Stack = {
    val nStack = super.lit(res,value,origin,stack)
    nStack.withType(res.id,res.typ)
  }

  override def fetch(res: AttrId, src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val nType = stack.getType(stack.resolve(src))
    val nStack = super.fetch(res, src, mode, origin, stack)
    nStack.withType(res,nType)
  }

  override def unpack(fields: Seq[AttrId], src: Ref, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val nType = stack.getType(stack.resolve(src))
    val argTypes = nType.projectionSeqMap {
      case adt:AdtType => adt.ctrs(context).headOption.map(_._2.values.toSeq).getOrElse(Seq.empty).padTo(fields.size, Type.Unknown(Set.empty)(origin))
      case _ => Seq.fill(fields.size)(Type.Unknown(Set.empty)(origin))
    }

    val nStack = super.unpack(fields, src, mode, origin, stack)
    fields.zip(argTypes).foldLeft(nStack){
      case (s, (id,cType)) => s.withType(id,cType)
    }
  }

  override def field(res: AttrId, src: Ref, fieldName: Id, mode: FetchMode, origin: SourceId, stack: Stack): Stack = {
    val nType = stack.getType(stack.resolve(src))
    val fieldType = nType.projectionMap{
      case adt:AdtType => adt.ctrs(context).headOption.flatMap(_._2.get(fieldName.name)).getOrElse(Type.Unknown(Set.empty)(origin))
      case _ => Type.Unknown(Set.empty)(origin)
    }
    val nStack = super.field(res, src, fieldName, mode, origin, stack)
    nStack.withType(res, fieldType)
  }

  override def pack(res:TypedId, srcs:Seq[Ref], ctr:Id, mode:FetchMode, origin:SourceId, stack: Stack): Stack = {
    val nStack = super.pack(res,srcs,ctr,mode,origin,stack)
    nStack.withType(res.id, res.typ)
  }

  override def invoke(res: Seq[AttrId], func: Func, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val defaultExtendedReturnTypes = func.returnInfo(context).padTo(res.size, Type.Unknown(Set.empty)(origin))

    val nStack = super.invoke(res, func, params, origin, stack)
    res.zip(defaultExtendedReturnTypes).foldLeft(nStack){
      case (s, (id,typ)) => s.withType(id,typ)
    }
  }

  override def invokeSig(res: Seq[AttrId], src: Ref, params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val defaultExtendedReturnTypes = stack.getType(src) match {
      case sdt:SigType => sdt.returnInfo(context).padTo(res.size, Type.Unknown(Set.empty)(origin))
      case _ => Seq.fill(res.size)(Type.Unknown(Set.empty)(origin))
    }

    val nStack = super.invokeSig(res, src, params, origin, stack)
    res.zip(defaultExtendedReturnTypes).foldLeft(nStack){
      case (s, (id,typ)) => s.withType(id,typ)
    }
  }

  override def rollback(res: Seq[AttrId], resTypes: Seq[Type], params: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val nStack = super.rollback(res, resTypes, params, origin, stack)
    res.zip(resTypes.padTo(res.size, Type.Unknown(Set.empty)(origin))).foldLeft(nStack){
      case (s, (id, typ)) => s.withType(id,typ)
    }
  }

  override def _return(res: Seq[AttrId], src: Seq[Ref], origin: SourceId, stack: Stack): Stack = {
    val argTypes = src.map(stack.getType(_)).padTo(res.size, Type.Unknown(Set.empty)(origin))
    val nStack = super._return(res, src, origin, stack)
    res.zip(argTypes).foldLeft(nStack){
      case (s, (id,cType)) => s.withType(id,cType)
    }
  }

  override def project(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val nType = stack.getType(stack.resolve(src)).projected(origin)
    val nStack = super.project(res, src, origin, stack)
    nStack.withType(res,nType)
  }

  override def unproject(res: AttrId, src: Ref, origin: SourceId, stack: Stack): Stack = {
    val nType = stack.getType(stack.resolve(src)) match {
      case typ:Type.Projected => typ.inner
      case _ => Type.Unknown(Set.empty)(origin)
    }
    val nStack = super.unproject(res, src, origin, stack)
    nStack.withType(res,nType)
  }
}

//Helps to defer decisions
//Is treated as an unknown unless handled specially by someone else
class JoinType(val joined:Set[Type])(origin:SourceId, attributes:Seq[Attribute] = Seq.empty) extends Unknown(Capability.all)(origin, attributes) {
  override def replaceContainedTypes(f: Type => Type): Type = {
    val newJoines = joined.map(f)
    if(newJoines.size == 1) {
      newJoines.head
    } else {
      new JoinType(newJoines)(origin, attributes)
    }
  }
  override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): JoinType = new JoinType(joined)(origin, attributes)
}

private object TypeTracker extends SlotDomain[Type] {
  override def merge(vs: Seq[Type]): Option[Type] = {
    assert(vs.nonEmpty)
    if(vs.forall(_ == vs.head)) {
      Some(vs.head)
    } else {
      Some(new JoinType(vs.toSet[Type].flatMap{
        case jt:JoinType=> jt.joined
        case t => Set(t)
      })(vs.head.src))
    }
  }
}

