package samaya.structure.types

import samaya.compilation.ErrorManager._
import samaya.structure.types.LitType.Projected
import samaya.structure.types.Type.DefaultProjected
import samaya.structure.{Attribute, Component, DataDef, FunctionSig, Generic, Module, ModuleEntry, Package}
import samaya.types.Context

import scala.collection.immutable.ListMap


//todo: we need origin (as Optional)
trait Type {
  def applies:Seq[Type] = Seq.empty
  def capabilities(context:Context):Set[Capability]
  def hasCap(context:Context, cap: Capability):Boolean = capabilities(context).contains(cap)
  def hasPermission(context: Context, perm:Permission): Boolean
  def isCurrentModule: Boolean
  def instantiate(applies:Seq[Type]): Type
  def replaceContainedTypes(f: Type => Type):Type
  def isUnknown = false
  def matches(other:Type):Option[Map[Int,Type]]
  def projected(src:SourceId,attributes:Seq[Attribute] = Seq.empty):Type
  def src:SourceId
  def attributes:Seq[Attribute]
  def changeMeta(src:SourceId = src, attributes:Seq[Attribute] = attributes):Type
  def projectionExtract[T](f:Type => T):T = f(this)
  def projectionMap(f:Type => Type):Type = f(this)
  def projectionSeqMap(f:Type => Seq[Type]):Seq[Type] = f(this)

  def asAdtType:Option[AdtType] = this match {
    case adtType: AdtType => Some(adtType)
    case _ => None
  }

  def asLitType:Option[LitType] = this match {
    case litType: LitType => Some(litType)
    case _ => None
  }

  def asSigType:Option[SigType] = this match {
    case sigType: SigType => Some(sigType)
    case _ => None
  }
  def prettyString(context: Context, genNames:Seq[String] = Seq.empty):String
}

trait DefinedType[D <: ModuleEntry] extends Type {
  def getPackage(context: Context): Option[Package]
  def getComponent(context:Context): Option[Component]
  def getEntry(context:Context):Option[D]
  //todo: use this everywhere instead of toString
  def prettyString(context:Context, genNames:Seq[String] = Seq.empty):String = {
    (getPackage(context), getComponent(context), getEntry(context)) match {
      case (Some(pkg), Some(comp), Some(entry)) =>
        s"${pkg.name}.${comp.name}.${entry.name}[${applies.map(_.prettyString(context,genNames)).mkString(",")}]"
      case _ => toString
    }
  }
}

//Specific types
sealed trait DataType extends DefinedType[DataDef] {
  override def instantiate(applies:Seq[Type]): DataType
}

sealed trait AdtType extends DataType {
  override def instantiate(applies:Seq[Type]): AdtType
  def ctrs(context:Context):ListMap[String,ListMap[String,Type]]
}

sealed trait LitType extends DataType {
  override def instantiate(applies:Seq[Type]): LitType
  def size(context:Context):Short
}

sealed trait SigType extends DefinedType[FunctionSig] with Func{
  override def replaceContainedTypes(f: Type => Type): SigType
  override def instantiate(applies:Seq[Type]): SigType
  def transactional(context:Context):Boolean
  def paramInfo(context:Context):Seq[(Type,Boolean)]
  def returnInfo(context:Context):Seq[Type]
  override def isUnknown: Boolean = false

  //Override conflict resolutions
  override def applies:Seq[Type] = Seq.empty
  override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): SigType

}

object Type {
  def restrictCapabilities(context:Context, base:Set[Capability], applies:Seq[(Generic,Type)]):Set[Capability] = {
    applies.foldLeft(base) { (r,gen) =>
      if(gen._1.phantom) {
        r
      } else {
        r.intersect(gen._2.capabilities(context))
      }
    }
  }

  def matchGenerics(matchers:Seq[Type], targets:Seq[Type]):Option[Map[Int,Type]] = {
    if(matchers.size != targets.size) return None
    var res = Map[Int, Type]()
    for((matcher, matched) <- matchers.zip(targets)) {
      val matchRes = matcher.matches(matched)
      if(matchRes.isEmpty) return None
      for((pos,typ) <- matchRes.get){
        if(res.contains(pos) && res(pos) != typ) return None
      }
      res = res ++ matchRes.get
    }
    Some(res)
  }

  case class GenericType(capabilities:Set[Capability], offset:Int)(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Type {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = DefaultProjected(this)(src, attributes)
    override def capabilities(context:Context): Set[Capability] = capabilities
    override def hasPermission(context: Context, perm: Permission): Boolean = false
    override def isCurrentModule: Boolean = false
    override def replaceContainedTypes(f: Type => Type): Type = this
    override def matches(other: Type): Option[Map[Int, Type]] = Some(Map(offset -> other))
    override def instantiate(applies: Seq[Type]): Type = {
      if(offset >= applies.size) {
        Unknown(capabilities)(src)
      } else {
        applies(offset)
      }
    }

    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): GenericType = GenericType(capabilities, offset)(src, attributes)
    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = {
      if(genNames.size > offset) {
        genNames(offset)
      } else {
        s"typeParam($offset)"
      }
    }
  }

  case class Unknown(capabilities:Set[Capability])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Type {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = DefaultProjected(this)(src, attributes)
    override def replaceContainedTypes(f: Type => Type): Type = this
    override def instantiate(applies: Seq[Type]): Type = this
    override def capabilities(context: Context): Set[Capability] = capabilities
    override def hasPermission(context: Context, perm: Permission): Boolean = false
    override def isCurrentModule: Boolean = false
    override def isUnknown = true
    override def matches(other: Type): Option[Map[Int, Type]] = None

    //ensure each instance is unique
    //is overwritten to make it clear in the code, that it is intended
    override def hashCode(): Int = System.identityHashCode(this)
    override def equals(obj: Any): Boolean = obj match {
      case value: Unknown => this.eq(value)
      case _ => false
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Unknown = Unknown(capabilities)(src, attributes)
    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = "unknown"
  }

  case class DefaultProjected(override val inner:Type)(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Projected {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = DefaultProjected(this)(src, attributes)
    override def projectionMap(f: Type => Type): Type = inner.projectionMap(f).projected(src,attributes)
    override def projectionSeqMap(f: Type => Seq[Type]): Seq[Type] = inner.projectionSeqMap(f).map(_.projected(src))
    override def replaceContainedTypes(f: Type => Type): Type = f(inner).projected(src, attributes)
    override def instantiate(applies: Seq[Type]): Type  = DefaultProjected(inner.instantiate(applies))(src, attributes)
    override def isCurrentModule: Boolean = inner.isCurrentModule
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): DefaultProjected = DefaultProjected(inner)(src, attributes)
  }


  trait Projected extends Type {
    def inner:Type
    override def capabilities(context:Context):Set[Capability] = Capability.all
    override def isCurrentModule: Boolean = inner.isCurrentModule
    override def hasPermission(context: Context, perm: Permission): Boolean = perm match {
      case Permission.Call | Permission.Define => false
      case _ => true
    }

    override def projectionExtract[T](f: Type => T): T = inner.projectionExtract(f)

    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case projected:Projected => inner.matches(projected.inner)
      case _ => None
    }

    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = s"projected(${inner.prettyString(context)})"

    def canEqual(other: Any): Boolean = other.isInstanceOf[Projected]

    //comparing / hashing only by name is on purpose as src is only met information
    override def equals(other: Any): Boolean = other match {
      case that: Projected =>  (that canEqual this) && inner == that.inner
      case _ => false
    }

  }



  trait LocalLookup[D <: ModuleEntry] extends DefinedType[D] {
    def offset:Int
    def select(module:Module, offset:Int):Option[D]
    override def isCurrentModule = true

    override def getPackage(context: Context): Option[Package] = Some(context.pkg)
    override def getComponent(context: Context): Option[Module] = context.module
    override def getEntry(context: Context): Option[D] = getComponent(context).flatMap(select(_, offset))
    def onDef[T](context: Context, fallback: T, f: (D, Type => Type) => T): T = {
      getEntry(context) match {
        case Some(typ) => f(typ, x => x)
        case None => fallback
      }
    }
  }

  def globalizeLocals(localModule:CompLink, typ:Type):Type = {
    def adaptLocals(typ:Type):Type = typ match {
      case AdtType.Local(lOffset, lApplies) => AdtType.Remote(localModule, lOffset, lApplies.map(adaptLocals))(typ.src)
      case LitType.Local(lOffset, lApplies) => LitType.Remote(localModule, lOffset, lApplies.map(adaptLocals))(typ.src)
      case SigType.Local(lOffset, lApplies) => SigType.Remote(localModule, lOffset, lApplies.map(adaptLocals))(typ.src)
      case AdtType.Projected(inner) => AdtType.Projected(adaptLocals(inner).asInstanceOf[AdtType])(typ.src)
      case LitType.Projected(inner) => LitType.Projected(adaptLocals(inner).asInstanceOf[LitType])(typ.src)
      case Type.DefaultProjected(inner) => Type.DefaultProjected(adaptLocals(inner))(typ.src)
      case AdtType.Remote(modHash, lOffset, lApplies) => AdtType.Remote(modHash, lOffset, lApplies.map(adaptLocals))(typ.src)
      case LitType.Remote(modHash, lOffset, lApplies) => LitType.Remote(modHash, lOffset, lApplies.map(adaptLocals))(typ.src)
      case SigType.Remote(modHash, lOffset, lApplies) => SigType.Remote(modHash, lOffset, lApplies.map(adaptLocals))(typ.src)
      case gen:GenericType => gen
      case nk:Type.Unknown => nk
    }
    adaptLocals(typ)
  }

  trait RemoteLookup[D <: ModuleEntry] extends DefinedType[D] {
    def moduleRef:CompLink
    def offset:Int
    def select(module:Module, offset:Int):Option[D]
    override def isCurrentModule: Boolean = false
    def adaptLocals(typ:Type):Type = globalizeLocals(moduleRef, typ)
    override def getPackage(context: Context): Option[Package] = context.pkg.packageOfLink(moduleRef)
    override def getComponent(context: Context): Option[Component] = context.pkg.componentByLink(moduleRef)
    override def getEntry(context: Context): Option[D] = getComponent(context).flatMap(_.asModule).flatMap(select(_,offset))
    def onDef[T](context: Context, fallback: T, f: (D, Type => Type) => T): T = {
      getEntry(context) match {
        case Some(typ) => f(typ, adaptLocals)
        case None => fallback
      }
    }
  }
}

object DataType {
  sealed trait Base extends Type {
    def onDef[T](context: Context, fallback: T, f: (DataDef, Type => Type) => T): T
    def select(module:Module, offset:Int): Option[DataDef] = module.dataType(offset)
    override def capabilities(context: Context): Set[Capability] = onDef(context, Set.empty, (dataType, _) => {
      Type.restrictCapabilities(context, dataType.capabilities, dataType.generics.zip(applies))
    })

    override def hasPermission(context: Context, perm:Permission): Boolean = onDef(context, false, (dataType, _) => {
      dataType.accessibility.get(perm) match {
        case None => false
        case Some(Accessibility.Global) => true
        case Some(Accessibility.Local) => isCurrentModule
        case Some(Accessibility.Guarded(guards)) => isCurrentModule || guards.forall { name =>
          dataType.generics.find(_.name == name) match {
            case Some(value) => applies(value.index).isCurrentModule
            case None => false
          }
        }
      }
    })
  }

  trait LocalLookup extends Base with Type.LocalLookup[DataDef]
  trait RemoteLookup extends Base with Type.RemoteLookup[DataDef]
}

object AdtType {
  sealed trait Declared extends AdtType with DataType.Base {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = Projected(this)(src, attributes)

    override def ctrs(context: Context): ListMap[String, ListMap[String, Type]] = onDef(context, ListMap.empty, (dataType, adaptType) => {
      ListMap(dataType.constructors.map(
        ctr => ctr.name -> ListMap(ctr.fields.map(
          field => field.name -> adaptType(field.typ).instantiate(applies)
        ): _*)
      ): _*)
    })
  }

  case class Local(override val offset: Int, override val applies: Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with DataType.LocalLookup {
    override def replaceContainedTypes(f: Type => Type): Type = Local(offset, this.applies.map(f))(src, attributes)
    override def instantiate(applies: Seq[Type]): AdtType = Local(offset, this.applies.map(a => a.instantiate(applies)))(src, attributes)
    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case AdtType.Local(otherOffset, otherApplies) if otherOffset ==  offset => Type.matchGenerics(applies, otherApplies)
      case _ => None
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Local = Local(offset, applies)(src, attributes)
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with DataType.RemoteLookup {
    override def replaceContainedTypes(f: Type => Type): Type = Remote(moduleRef,offset, this.applies.map(f))(src, attributes)
    override def instantiate(applies: Seq[Type]): AdtType = Remote(moduleRef,offset, this.applies.map(a => a.instantiate(applies)))(src, attributes)
    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case AdtType.Remote(otherModule, otherOffset, otherApplies) if otherModule == moduleRef && otherOffset == offset => Type.matchGenerics(applies, otherApplies)
      case _ => None
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Remote = Remote(moduleRef, offset, applies)(src, attributes)

  }

  case class Projected(override val inner:AdtType)(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends AdtType with Type.Projected {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = Projected(this)(src, attributes)
    override def projectionMap(f: Type => Type): Type = inner.projectionMap(f).projected(src,attributes)
    override def projectionSeqMap(f: Type => Seq[Type]): Seq[Type] = inner.projectionSeqMap(f).map(_.projected(src))
    override def ctrs(context: Context): ListMap[String,ListMap[String,Type]] = inner.ctrs(context).map{
      case (ctrName,fields) => ctrName -> fields.map{
        case (fieldName,fieldTyp) => fieldName -> fieldTyp.projected(fieldTyp.src, fieldTyp.attributes)
      }
    }
    override def replaceContainedTypes(f: Type => Type): Type = f(inner).projected(src, attributes)
    override def instantiate(applies: Seq[Type]): AdtType = Projected(inner.instantiate(applies))(src, attributes)
    override def getPackage(context: Context): Option[Package] = inner.getPackage(context)
    override def getComponent(context: Context): Option[Component] = inner.getComponent(context)
    override def getEntry(context:Context): Option[DataDef] = inner.getEntry(context)
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Projected = Projected(inner)(src, attributes)

  }


}

object LitType {

  sealed trait Declared extends LitType with DataType.Base {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = Projected(this)(src, attributes)
    override def size(context:Context):Short = onDef(context, 0, (dataType, _) => dataType.external.get)
  }

  case class Local(override val offset: Int, override val applies: Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with DataType.LocalLookup {
    override def replaceContainedTypes(f: Type => Type): Local = Local(offset, this.applies.map(f))(src, attributes)
    override def instantiate(applies: Seq[Type]): LitType = Local(offset, this.applies.map(a => a.instantiate(applies)))(src, attributes)
    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case LitType.Local(otherOffset, otherApplies) if otherOffset == offset => Type.matchGenerics(applies, otherApplies)
      case _ => None
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Local = Local(offset, applies)(src, attributes)
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with DataType.RemoteLookup {
    override def replaceContainedTypes(f: Type => Type): Remote =  Remote(moduleRef,offset, this.applies.map(f))(src, attributes)
    override def instantiate(applies: Seq[Type]): LitType = Remote(moduleRef,offset, this.applies.map(a => a.instantiate(applies)))(src, attributes)
    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case LitType.Remote(otherModule, otherOffset, otherApplies) if otherModule == moduleRef && otherOffset == offset => Type.matchGenerics(applies, otherApplies)
      case _ => None
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Remote = Remote(moduleRef, offset, applies)(src, attributes)
  }

  case class Projected(override val inner:LitType)(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends LitType with Type.Projected {
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = Projected(this)(src,attributes)
    override def projectionMap(f: Type => Type): Type = inner.projectionMap(f).projected(src,attributes)
    override def projectionSeqMap(f: Type => Seq[Type]): Seq[Type] = inner.projectionSeqMap(f).map(_.projected(src))
    override def size(context:Context): Short = inner.size(context)
    override def replaceContainedTypes(f: Type => Type): Type = f(inner).projected(src,attributes)
    override def instantiate(applies: Seq[Type]): LitType = Projected(inner.instantiate(applies))(src, attributes)
    override def getPackage(context: Context): Option[Package] = inner.getPackage(context)
    override def getComponent(context:Context): Option[Component] = inner.getComponent(context)
    override def getEntry(context:Context): Option[DataDef] = inner.getEntry(context)
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Projected = Projected(inner)(src, attributes)

  }

}

object SigType {

  sealed trait Declared extends SigType {
    def onDef[T](context: Context, fallback: T, f: (FunctionSig, Type => Type) => T): T
    override def projected(src:SourceId, attributes:Seq[Attribute] = Seq.empty): Type = Type.DefaultProjected(this)(src, attributes)
    def transactional(context:Context): Boolean = onDef(context, false, (sigDef, _ ) => sigDef.transactional)

    def paramInfo(context:Context):Seq[(Type, Boolean)] = {
      onDef(context, Seq.empty, (sigDef, adaptType) => sigDef.params.map(p => (adaptType(p.typ).instantiate(applies),p.consumes)))
    }

    def returnInfo(context:Context):Seq[Type] = {
      onDef(context, Seq.empty, (sigDef, adaptType) => sigDef.results.map(r => adaptType(r.typ).instantiate(applies)))
    }

    def name(context:Context): String = onDef(context,"unknown", (func, _) => func.name)
    def select(module: Module, offset: Int): Option[FunctionSig] = module.signature(offset)
    override def capabilities(context: Context): Set[Capability] = onDef(context, Set.empty, (sigType, _) => sigType.capabilities)

    override def hasPermission(context: Context, perm:Permission): Boolean = onDef(context, false, (sigDef, _) => {
      sigDef.accessibility.get(perm) match {
        case None => false
        case Some(Accessibility.Global) => true
        case Some(Accessibility.Local) => isCurrentModule
        case Some(Accessibility.Guarded(guards)) => isCurrentModule || guards.forall { name =>
          sigDef.generics.find(_.name == name) match {
            case Some(value) => applies(value.index).isCurrentModule
            case None =>false
          }
        }
      }
    })
  }

  case class Local(override val offset: Int, override val applies: Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with Type.LocalLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): Local = Local(offset, this.applies.map(f))(src, attributes)
    override def instantiate(applies: Seq[Type]): SigType = Local(offset, this.applies.map(a => a.instantiate(applies)))(src, attributes)
    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case SigType.Local(otherOffset, otherApplies) if otherOffset == offset => Type.matchGenerics(applies, otherApplies)
      case _ => None
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Local = Local(offset, applies)(src, attributes)

  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with Type.RemoteLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): Remote = Remote(moduleRef,offset, this.applies.map(f))(src, attributes)
    override def instantiate(applies: Seq[Type]): SigType = Remote(moduleRef,offset, this.applies.map(a => a.instantiate(applies)))(src, attributes)
    override def matches(other: Type): Option[Map[Int, Type]] = other match {
      case SigType.Remote(otherModule, otherOffset, otherApplies) if otherModule == moduleRef && otherOffset == offset => Type.matchGenerics(applies, otherApplies)
      case _ => None
    }
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Remote = Remote(moduleRef, offset, applies)(src, attributes)

  }
}