package samaya.structure.types

import samaya.compilation.ErrorManager._
import samaya.structure.types.Func.Unknown
import samaya.structure.types.Type.{GenericType, Unknown}
import samaya.structure.{Attribute, Component, FunctionSig, Generic, Module, ModuleEntry, Package}
import samaya.types.Context

trait Func {
  def applies:Seq[Type] = Seq.empty
  def paramInfo(context:Context):Seq[(Type,Boolean)]
  def returnInfo(context:Context):Seq[Type]
  def isCurrentModule: Boolean
  def name(context:Context): String
  def hasPermission(context: Context, perm:Permission): Boolean
  def replaceContainedTypes(f: Type => Type):Func
  def transactional(context:Context):Boolean
  def isUnknown = false
  def src:SourceId
  def attributes:Seq[Attribute]
  def changeMeta(src:SourceId = src, attributes:Seq[Attribute] = attributes):Func


  def asStdFunc:Option[StdFunc] = this match {
    case stdFunc: StdFunc => Some(stdFunc)
    case _ => None
  }

  def asImplFunc:Option[ImplFunc] = this match {
    case implFunc: ImplFunc => Some(implFunc)
    case _ => None
  }

  def prettyString(context: Context, genNames:Seq[String] = Seq.empty):String

}

trait DefinedFunc[D <: ModuleEntry] extends Func {
  def definingComp:Option[CompLink]
  def getPackage(context:Context): Option[Package]
  def getComponent(context:Context): Option[Component]
  def getEntry(context:Context):Option[D]
  def prettyString(context:Context, genNames:Seq[String] = Seq.empty):String = {
    (getPackage(context), getComponent(context), getEntry(context)) match {
      case (Some(pkg), Some(comp), Some(entry)) =>
        s"${pkg.name}.${comp.name}.${entry.name}[${applies.map(_.prettyString(context,genNames)).mkString(",")}]"
      case _ => toString
    }
  }
}

sealed trait StdFunc extends DefinedFunc[FunctionSig] {
  def replaceContainedTypes(f: Type => Type):StdFunc
}

sealed trait ImplFunc extends DefinedFunc[FunctionSig] {
  def replaceContainedTypes(f: Type => Type):ImplFunc
}

object Func {

  case class Unknown(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Func {
    def replaceContainedTypes(f: Type => Type):this.type = this
    //ensure each instance is unique
    //is overwritten to make it clear in the code, that it is intended
    override def hashCode(): Int = System.identityHashCode(this)
    override def equals(obj: Any): Boolean = obj match {
      case value: Unknown => this.eq(value)
      case _ => false
    }

    override def isUnknown = true
    override def hasPermission(context: Context, perm: Permission): Boolean = false
    override def isCurrentModule: Boolean = false
    override def paramInfo(context: Context): Seq[(Type, Boolean)] = Seq.empty
    override def returnInfo(context: Context): Seq[Type] = Seq.empty
    override def name(context: Context): String = "unknown"
    override def transactional(context: Context): Boolean = false
    override def prettyString(context: Context, genNames:Seq[String] = Seq.empty): String = "unknown"
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Unknown = Unknown(src, attributes)
  }

  def globalizeLocals(localModule:CompLink, func:Func):Func = {
    (func match {
      case ImplFunc.Local(index, applies) => ImplFunc.Remote(localModule, index, applies)(func.src)
      case StdFunc.Local(index, applies) => StdFunc.Remote(localModule, index, applies)(func.src)
      case sigType: SigType => return Type.globalizeLocals(localModule, sigType).asInstanceOf[SigType]
      case fun => fun
    }).replaceContainedTypes(Type.globalizeLocals(localModule,_))
  }

  trait LocalLookup[D <: ModuleEntry] extends DefinedFunc[D] {
    def offset:Int
    def select(module:Module, offset:Int):Option[D]
    override def isCurrentModule = true
    override def definingComp:Option[CompLink] = None
    override def getPackage(context: Context): Option[Package] = Some(context.pkg)
    override def getComponent(context: Context): Option[Component] = context.module
    override def getEntry(context: Context): Option[D] = context.module.flatMap(select(_, offset))
    def onDef[T](context: Context, fallback: T, f: (D, Type => Type) => T): T = {
      getEntry(context) match {
        case Some(func) => f(func, x => x)
        case None => fallback
      }
    }
  }

  //todo: shared suprtype with Type
  trait RemoteLookup[D <: ModuleEntry] extends DefinedFunc[D] {
    def moduleRef:CompLink
    def offset:Int
    def select(module:Module, offset:Int):Option[D]
    override def isCurrentModule: Boolean = false
    override def definingComp:Option[CompLink] = Some(moduleRef)
    def adaptLocals(typ:Type):Type = Type.globalizeLocals(moduleRef,typ)
    override def getPackage(context: Context): Option[Package] = context.pkg.packageOfLink(moduleRef)
    override def getComponent(context: Context): Option[Component] = context.pkg.componentByLink(moduleRef)
    override def getEntry(context: Context): Option[D] = context.pkg.componentByLink(moduleRef).flatMap(_.asModule).flatMap(select(_,offset))
    def onDef[T](context: Context, fallback: T, f: (D, Type => Type) => T): T = {
      getEntry(context)  match {
        case Some(func) => f(func, adaptLocals)
        case None =>
          fallback
      }
    }
  }
}

object StdFunc {

  sealed trait Declared extends StdFunc {
    def onDef[T](context: Context, fallback: T, f: (FunctionSig, Type => Type) => T): T
    def transactional(context:Context): Boolean = onDef(context, false, (funDef, _ ) => funDef.transactional)
    def select(module: Module, offset: Int): Option[FunctionSig] = module.function(offset)

    def paramInfo(context:Context): Seq[(Type, Boolean)] = {
      onDef(context,Seq.empty,(f,adaptType) => f.params.map(p => (adaptType(p.typ).instantiate(applies), p.consumes)))
    }

    def returnInfo(context:Context): Seq[Type] = {
      onDef(context,Seq.empty, (func, adaptType) => func.results.map(p => adaptType(p.typ).instantiate(applies)))
    }

    def name(context:Context): String = {
      onDef(context,"unknown", (func, _) => func.name)
    }

    override def hasPermission(context: Context, perm:Permission): Boolean = onDef(context, false, (sig, _) => {
      sig.accessibility.get(perm) match {
        case None => false
        case Some(Accessibility.Global) => true
        case Some(Accessibility.Local) => isCurrentModule
        case Some(Accessibility.Guarded(guards)) => isCurrentModule || guards.forall { name =>
          sig.generics.find(_.name == name) match {
            case Some(value) => applies(value.index).isCurrentModule
            case None => false
          }
        }
      }
    })

  }

  case class Local(override val offset: Int, override val applies: Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with Func.LocalLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): StdFunc = Local(offset, this.applies.map(f))(src,attributes)
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Local = Local(offset,applies)(src, attributes)
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with Func.RemoteLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): StdFunc = Remote(moduleRef,offset, this.applies.map(f))(src,attributes)
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Remote = Remote(moduleRef, offset,applies)(src, attributes)
  }
}


object ImplFunc {

  sealed trait Declared extends ImplFunc {
    def onDef[T](context: Context, fallback: T, f: (FunctionSig, Type => Type) => T): T
    def transactional(context:Context): Boolean = onDef(context, false, (funDef, _ ) => funDef.transactional)
    def select(module: Module, offset: Int): Option[FunctionSig] = module.implement(offset)

    def paramInfo(context:Context): Seq[(Type, Boolean)] = {
      onDef(context,Seq.empty,(f,adaptType) => f.params.map(p => (adaptType(p.typ).instantiate(applies), p.consumes)))
    }

    def returnInfo(context:Context): Seq[Type] = {
      onDef(context,Seq.empty, (func, adaptType) => func.results.map(p => adaptType(p.typ).instantiate(applies)))
    }

    def name(context:Context): String = {
      onDef(context,"unknown", (func, _) => func.name)
    }

    override def hasPermission(context: Context, perm:Permission): Boolean = onDef(context, false, (sig, _) => {
      sig.accessibility.get(perm) match {
        case None => false
        case Some(Accessibility.Global) => true
        case Some(Accessibility.Local) => isCurrentModule
        case Some(Accessibility.Guarded(guards)) => isCurrentModule || guards.forall { name =>
          sig.generics.find(_.name == name) match {
            case Some(value) => applies(value.index).isCurrentModule
            case None => false
          }
        }
      }
    })
  }

  case class Local(override val offset: Int, override val applies: Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with Func.LocalLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): ImplFunc = Local(offset, this.applies.map(f))(src,attributes)
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Local = Local(offset,applies)(src, attributes)
  }

  case class Remote(override val moduleRef:CompLink, override val offset:Int, override val applies:Seq[Type])(override val src:SourceId, override val attributes:Seq[Attribute] = Seq.empty) extends Declared with Func.RemoteLookup[FunctionSig] {
    override def replaceContainedTypes(f: Type => Type): ImplFunc = Remote(moduleRef,offset, this.applies.map(f))(src,attributes)
    override def changeMeta(src: SourceId = src, attributes: Seq[Attribute] = attributes): Remote = Remote(moduleRef, offset,applies)(src, attributes)
  }
}