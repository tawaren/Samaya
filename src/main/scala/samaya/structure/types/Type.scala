package samaya.structure.types

import samaya.compilation.ErrorManager._
import samaya.structure.{Component, DataDef, FunctionSig, Generic, Module, ModuleEntry, Package}
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

  def projected():Type

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

}

object Type {
  val errorType:Type = new Unknown(Set.empty)
  object DefaultUnknown extends Unknown(Capability.all)

  def restrictCapabilities(context:Context, base:Set[Capability], applies:Seq[(Generic,Type)]):Set[Capability] = {
    applies.foldLeft(base) { (r,gen) =>
      if(gen._1.phantom) {
        base
      } else {
        base.intersect(gen._2.capabilities(context))
      }
    }
  }

  case class GenericType(capabilities:Set[Capability], offset:Int) extends Type {
    override def projected(): Type = DefaultProjected(this)
    override def capabilities(context:Context): Set[Capability] = capabilities
    override def hasPermission(context: Context, perm: Permission): Boolean = false
    override def isCurrentModule: Boolean = false
    override def replaceContainedTypes(f: Type => Type): Type = this
    override def instantiate(applies: Seq[Type]): Type = {
      if(offset >= applies.size) {
        feedback(PlainMessage(s"No instantiation for generic type provided during substitution", Error))
        Type.errorType
      } else {
        applies(offset)
      }
    }

    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = {
      if(genNames.size > offset) {
        genNames(offset)
      } else {
        s"typeParam($offset)"
      }
    }
  }

  class Unknown(capabilities:Set[Capability]) extends Type {
    override def projected(): Type = DefaultProjected(this)
    override def replaceContainedTypes(f: Type => Type): Type = this
    override def instantiate(applies: Seq[Type]): Type = this
    override def capabilities(context: Context): Set[Capability] = capabilities
    override def hasPermission(context: Context, perm: Permission): Boolean = false
    override def isCurrentModule: Boolean = false
    override def isUnknown = true

    //ensure each instance is unique
    //is overwritten to make it clear in the code, that it is intended
    override def hashCode(): Int = System.identityHashCode(this)
    override def equals(obj: Any): Boolean = obj match {
      case value: Unknown => this.eq(value)
      case _ => false
    }
    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = "unknown"
  }

  trait Projected extends Type {
    def inner:Type
    override def capabilities(context:Context):Set[Capability] = Capability.all
    override def isCurrentModule: Boolean = inner.isCurrentModule
    override def hasPermission(context: Context, perm: Permission): Boolean = perm match {
      case Permission.Call | Permission.Define => false
      case _ => true
    }
    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = s"projected(${inner.prettyString(context)})"
  }

  case class DefaultProjected(override val inner:Type) extends Projected {
    override def projected(): Type = DefaultProjected(this)
    override def replaceContainedTypes(f: Type => Type): Type = f(inner).projected()
    override def instantiate(applies: Seq[Type]): Type  = DefaultProjected(inner.instantiate(applies))
    override def isCurrentModule: Boolean = inner.isCurrentModule
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
        case None =>
          feedback(PlainMessage(s"Can not find dependent data type with index: $offset in current module", Error))
          fallback
      }
    }
  }

  def globalizeLocals(localModule:CompLink, typ:Type):Type = {
    def adaptLocals(typ:Type):Type = typ match {
      case AdtType.Local(lOffset, lApplies) => AdtType.Remote(localModule, lOffset, lApplies.map(adaptLocals))
      case LitType.Local(lOffset, lApplies) => LitType.Remote(localModule, lOffset, lApplies.map(adaptLocals))
      case SigType.Local(lOffset, lApplies) => SigType.Remote(localModule, lOffset, lApplies.map(adaptLocals))
      case AdtType.Projected(inner) => AdtType.Projected(adaptLocals(inner).asInstanceOf[AdtType])
      case LitType.Projected(inner) => LitType.Projected(adaptLocals(inner).asInstanceOf[LitType])
      case Type.DefaultProjected(inner) => Type.DefaultProjected(adaptLocals(inner))
      case AdtType.Remote(modHash, lOffset, lApplies) => AdtType.Remote(modHash, lOffset, lApplies.map(adaptLocals))
      case LitType.Remote(modHash, lOffset, lApplies) => LitType.Remote(modHash, lOffset, lApplies.map(adaptLocals))
      case SigType.Remote(modHash, lOffset, lApplies) => SigType.Remote(modHash, lOffset, lApplies.map(adaptLocals))
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
        case None =>
          feedback(PlainMessage(s"Can not find dependent module with hash: $moduleRef", Error))
          fallback
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
        case Some(Accessibility.Guarded(guards)) => guards.forall { name =>
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
    override def projected(): Type = Projected(this)
    override def ctrs(context: Context): ListMap[String, ListMap[String, Type]] = onDef(context, ListMap.empty, (dataType, adaptType) => {
      ListMap(dataType.constructors.map(
        ctr => ctr.name -> ListMap(ctr.fields.map(
          field => field.name -> adaptType(field.typ).instantiate(applies)
        ): _*)
      ): _*)
    })
  }

  case class Local(override val offset: Int, override val applies: Seq[Type]) extends Declared with DataType.LocalLookup {
    override def replaceContainedTypes(f: Type => Type): Type = Local(offset, this.applies.map(f))
    override def instantiate(applies: Seq[Type]): AdtType = Local(offset, this.applies.map(a => a.instantiate(applies)))
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type]) extends Declared with DataType.RemoteLookup {
    override def replaceContainedTypes(f: Type => Type): Type = Remote(moduleRef,offset, this.applies.map(f))
    override def instantiate(applies: Seq[Type]): AdtType = Remote(moduleRef,offset, this.applies.map(a => a.instantiate(applies)))
  }

  case class Projected(override val inner:AdtType) extends AdtType with Type.Projected {
    override def projected(): Type = Projected(this)
    override def hasPermission(context: Context, perm: Permission): Boolean = inner.hasPermission(context, perm)
    override def ctrs(context: Context): ListMap[String,ListMap[String,Type]] = inner.ctrs(context).map{
      case (ctrName,fields) => ctrName -> fields.map{
        case (fieldName,fieldTyp) => fieldName -> fieldTyp.projected()
      }
    }

    override def replaceContainedTypes(f: Type => Type): Type = f(inner).projected()
    override def instantiate(applies: Seq[Type]): AdtType = Projected(inner.instantiate(applies))
    override def getPackage(context: Context): Option[Package] = inner.getPackage(context)
    override def getComponent(context: Context): Option[Component] = inner.getComponent(context)
    override def getEntry(context:Context): Option[DataDef] = inner.getEntry(context)
  }


}

object LitType {

  sealed trait Declared extends LitType with DataType.Base {
    override def projected(): Type = Projected(this)
    override def size(context:Context):Short = onDef(context, 0, (dataType, _) => dataType.external.get)
  }

  case class Local(override val offset: Int, override val applies: Seq[Type]) extends Declared with DataType.LocalLookup {
    override def replaceContainedTypes(f: Type => Type): Local = Local(offset, this.applies.map(f))
    override def instantiate(applies: Seq[Type]): LitType = Local(offset, this.applies.map(a => a.instantiate(applies)))
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type]) extends Declared with DataType.RemoteLookup {
    override def replaceContainedTypes(f: Type => Type): Remote =  Remote(moduleRef,offset, this.applies.map(f))
    override def instantiate(applies: Seq[Type]): LitType = Remote(moduleRef,offset, this.applies.map(a => a.instantiate(applies)))
  }

  case class Projected(override val inner:LitType) extends LitType with Type.Projected {
    override def projected(): Type = Projected(this)
    override def size(context:Context): Short = inner.size(context)
    override def replaceContainedTypes(f: Type => Type): Type = f(inner).projected()
    override def instantiate(applies: Seq[Type]): LitType = Projected(inner.instantiate(applies))
    override def getPackage(context: Context): Option[Package] = inner.getPackage(context)
    override def getComponent(context:Context): Option[Component] = inner.getComponent(context)
    override def getEntry(context:Context): Option[DataDef] = inner.getEntry(context)
  }

}


object SigType {

  sealed trait Declared extends SigType {
    def onDef[T](context: Context, fallback: T, f: (FunctionSig, Type => Type) => T): T
    override def projected(): Type = Type.DefaultProjected(this)
    def transactional(context:Context): Boolean = onDef(context, false, (sigDef, _ ) => sigDef.transactional)

    def paramInfo(context:Context):Seq[(Type, Boolean)] = {
      onDef(context, Seq.empty, (sigDef, adaptType) => sigDef.params.map(p => (adaptType(p.typ).instantiate(applies),p.consumes)))
    }

    def returnInfo(context:Context):Seq[Type] = {
      onDef(context, Seq.empty, (sigDef, adaptType) => sigDef.results.map(r => adaptType(r.typ).instantiate(applies)))
    }

    def name(context:Context): String = onDef(context,"unknown", (func, _) => func.name)
    def select(module: Module, offset: Int): Option[FunctionSig] = module.signature(offset)
    override def capabilities(context: Context): Set[Capability] = onDef(context, Set.empty, (sigType, _) => {
      Type.restrictCapabilities(context, sigType.capabilities, sigType.generics.zip(applies))
    })

    override def hasPermission(context: Context, perm:Permission): Boolean = onDef(context, false, (sigDef, _) => {
      sigDef.accessibility.get(perm) match {
        case None => false
        case Some(Accessibility.Global) => true
        case Some(Accessibility.Local) => isCurrentModule
        case Some(Accessibility.Guarded(guards)) => guards.forall { name =>
          sigDef.generics.find(_.name == name) match {
            case Some(value) => applies(value.index).isCurrentModule
            case None =>false
          }
        }
      }
    })
  }

  case class Local(override val offset: Int, override val applies: Seq[Type]) extends Declared with Type.LocalLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): Local = Local(offset, this.applies.map(f))
    override def instantiate(applies: Seq[Type]): SigType = Local(offset, this.applies.map(a => a.instantiate(applies)))
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type]) extends Declared with Type.RemoteLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): Remote = Remote(moduleRef,offset, this.applies.map(f))
    override def instantiate(applies: Seq[Type]): SigType = Remote(moduleRef,offset, this.applies.map(a => a.instantiate(applies)))
  }
}