package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Error, LocatedMessage, PlainMessage, Warning, feedback, unexpected}
import samaya.plugin.impl.compiler.mandala.MandalaCompiler
import samaya.plugin.impl.compiler.mandala.components.clazz.Class
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.simple.MandalaParser
import samaya.structure.types.{AdtType, Capability, Func, ImplFunc, LitType, SigType, SourceId, StdFunc, Type}
import samaya.structure.{Attribute, Component, DataDef, FunctionDef, FunctionSig, Generic, ImplementDef, Interface, Module, Package, SignatureDef}
import samaya.toolbox.process.TypeInference
import samaya.toolbox.process.TypeInference.TypeVar

import scala.collection.JavaConverters._

trait ComponentResolver extends CompilerToolbox {
  self: ComponentBuilder with CapabilityCompiler =>

  private var imports: Map[String, Seq[String]] = Map[String,Seq[String]]()
  //This is per component
  private var availableGenerics = Map.empty[String,Generic]

  def withGenerics[T](generics:Seq[Generic])(body: => T):T={
    val oldGenerics =  availableGenerics
    availableGenerics = generics.map(g => (g.name,g)).toMap
    val res = body
    availableGenerics = oldGenerics
    res
  }

  override def visitGenericArgs(ctx: MandalaParser.GenericArgsContext): Seq[Generic] = {
    //this spares the caller the check and simplifies the code
    if(ctx == null) return Seq.empty
    //reset the index
    withFreshIndex {
      ctx.generics.asScala.map(visitGenericDef)
    }
  }

  override def visitGenericDef(ctx: MandalaParser.GenericDefContext): Generic = new Generic {
    override val name: String = visitName(ctx.name)
    override val index: Int = nextIndex()
    override val phantom: Boolean = ctx.PHANTOM() != null
    override val capabilities: Set[Capability] = visitCapabilities(ctx.capabilities())
    override val attributes: Seq[Attribute] = Seq.empty
    override val src: SourceId = sourceIdFromContext(ctx)
  }

  private def addWildCardImport(elems:Seq[String], sourceId: SourceId): Unit ={
    val mod = elems.last
    val path = elems.init
    //First check if it is defined in Mandala we prefer these
    val components = env.pkg.componentByPathAndName(path,mod).flatMap(_.asModule)
    if(components.isEmpty) {
      feedback(PlainMessage(s"Wildcard import ${elems.mkString(".")} does not point to a component", Warning))
      return
    }
    for(module <- components) {
      for(c <- module.functions ++ module.dataTypes ++ module.signatures ++ module.implements) {
        recordImport(elems :+ c.name, sourceId)
      }
      //In case it is a Mandala Module we can import the instances as well
      module match {
        case mandalaModule: MandalaModule =>
          for( clazz <- mandalaModule.instances.values; instName <- clazz;
            inst <- env.pkg.componentByPathAndName(path,instName)
          ){
            inst match {
              case defInstance: DefInstance =>  {
                env.instRec(defInstance, isLocal = false)
              }
              case _ =>
            }
          }
        case _ =>
      }
    }
  }

  private def addImport(elems:Seq[String], sourceId: SourceId): Unit ={
    recordImport(elems, sourceId)
    val mod = elems.last
    val path = elems.init

    env.pkg.componentByPathAndName(path,mod).foreach {
      case defInstance:DefInstance =>
        env.instRec(defInstance, isLocal = false)
      case _ =>
    }

    if(path.nonEmpty) {
      val lookup = env.pkg.componentByPathAndName(path.init,Instance.deriveTopName(path.last,mod))
      env.pkg.componentByPathAndName(path.init,Instance.deriveTopName(path.last,mod)).foreach{
        case defInstance:DefInstance => env.instRec(defInstance, isLocal = false)
        case _ =>
      }
    }

  }

  private def recordImport(elems:Seq[String], sourceId: SourceId): Unit ={
    val last = elems.last
    if(imports.contains(last) && imports(last) != elems) {
      feedback(LocatedMessage(s"Import ${elems.mkString(".")} shadows a previous import", sourceId, Warning))
    }
    imports = imports.updated(last,elems)
  }

  override def visitImport_(ctx: MandalaParser.Import_Context): Unit = {
    val elems = ctx.path().part.asScala.map(visitName)
    val src = sourceIdFromContext(ctx);
    if(ctx.wildcard() != null) {
      addWildCardImport(elems, src)
    } else {
      addImport(elems, src)
    }
  }


  override def visitTypeRefArgs(ctx: MandalaParser.TypeRefArgsContext): Seq[Type] = {
    ctx.targs.asScala.map(visitTypeRef)
  }

  override def visitBaseRef(ctx: MandalaParser.BaseRefContext): Type = {
    val parts = ctx.path().part.asScala.map(visitName)
    //Resolve arguments
    val applies = if(ctx.targs == null) None else Some(ctx.targs.targs.asScala.map(visitTypeRef))
    resolveType(parts, applies, sourceIdFromContext(ctx))
  }

  def visitFunRef(ctx: MandalaParser.BaseRefContext): Func = {
    val parts = ctx.path().part.asScala.map(visitName)
    //Resolve arguments
    val applies = if(ctx.targs == null) None else Some(ctx.targs.targs.asScala.map(visitTypeRef))
    resolveFunc(parts, applies, sourceIdFromContext(ctx))
  }

  override def visitTypeRef(ctx: MandalaParser.TypeRefContext): Type = {
    if(ctx == null) {
      new TypeInference.TypeVar()
    }else if(ctx.PROJECT() != null) {
      visitTypeRef(ctx.typeRef()).projected()
    } else if(ctx.QUEST() != null) {
      new TypeVar
    } else {
      visitBaseRef(ctx.baseRef())
    }
  }

  def resolveImport(importPath:Seq[String], isEntryPath:Boolean = true): (Seq[String], Seq[Interface[Component]]) = {
    val start = importPath.head
    //If not a local declaration it can still be something imported | absolute path
    //todo: shall we check both, original as well?
    val path = if(imports.contains(start)) {
      imports(start) ++ importPath.tail
    } else {
      importPath
    }

    val (componentName, packagePath) = if(isEntryPath) {
      if(path.size >= 2) {
        (path.init.last, path.init.init)
      } else {
        return (path, Seq.empty)
      }
    } else {
      (path.last, path.init)
    }

    //Find all modules matching the path
    (path, env.pkg.findComponentByPath(packagePath, Package.nameFilter(componentName)))
  }

  private def resolveApplies(applies:Option[Seq[Type]], classVars:Int, expected:Int):Seq[Type] = {
    assert(classVars <= expected)
    applies match {
      case Some(value) => Seq.fill(classVars)(new TypeVar) ++ value
      case None => Seq.fill(expected)(new TypeVar)
    }
  }

  //The resolver algorithms
  def resolveType(importPath:Seq[String],applies:Option[Seq[Type]], sourceId:SourceId): Type = {
    val start = importPath.head
    //If it is just a name not a path it can refer to local declarations
    if(importPath.size == 1) {
      //May be from local scope
      if(availableGenerics.contains(start)){
        //check that there are no params
        if(applies.nonEmpty) feedback(LocatedMessage(s"Generics do not have parameters", sourceId, Error))
        val generic = availableGenerics(start)
        return Type.GenericType(generic.capabilities,generic.index)
      }

      //May be from local module scope
      if(localEntries.contains(start)){
        return localEntries(start) match {
          case dt:DataDef if dt.external.isDefined => LitType.Local(dt.index, resolveApplies(applies, 0, dt.generics.size))
          case dt:DataDef if dt.external.isEmpty => AdtType.Local(dt.index, resolveApplies(applies, 0, dt.generics.size))
          case st:SignatureDef => SigType.Local(st.index, resolveApplies(applies, 0, st.generics.size))
          case _:FunctionDef =>
            feedback(LocatedMessage(s"$start does refer to a function but a type is expected", sourceId, Error))
            Type.DefaultUnknown
          case _:ImplementDef =>
            feedback(LocatedMessage(s"$start does refer to an implement but a type is expected", sourceId, Error))
            Type.DefaultUnknown
        }
      }
    }
    val (path, comps) = resolveImport(importPath)
    val entryName = path.last
    val targets = comps.flatMap(_.asModuleInterface).flatMap(m => (m.dataTypes ++ m.signatures).filter(_.name == entryName).map((m,_)))
    //did we found at least 1
    if(targets.isEmpty) {
      if(comps.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error))
      } else {
        feedback(LocatedMessage(s"typ ${path.mkString(".")} does not exist", sourceId, Error))
      }
      return Type.DefaultUnknown
    }

    //did we found more than 1 candidate
    if(targets.size > 1) feedback(LocatedMessage(s"${path.mkString(".")} is ambiguous (points to multiple candidates)", sourceId, Error))
    //construct the type
    //Note: currently classes can not define DataTypes but we still add the getClassVars(module) as we may allow it in the future
    targets.head match {
      case (module,dt:DataDef) if dt.external.isDefined => LitType.Remote(module.link, dt.index, resolveApplies(applies, getClassVars(module), dt.generics.size))
      case (module,dt:DataDef) if dt.external.isEmpty => AdtType.Remote(module.link, dt.index, resolveApplies(applies, getClassVars(module), dt.generics.size))
      case (module,st:SignatureDef) => SigType.Remote(module.link, st.index, resolveApplies(applies, getClassVars(module), st.generics.size))
      case _ => Type.DefaultUnknown
    }
  }

  private def getClassVars(module:Module) = module match {
    case cls:Class => cls.classGenerics.size
    case _ => 0
  }

  def resolveFunc(importPath:Seq[String],applies:Option[Seq[Type]], sourceId: SourceId): Func = {
    val start = importPath.head
    //If it is just a name not a path it can refer to local declarations
    if(importPath.size == 1) {
      //May be from local module scope
      if(localEntries.contains(start)){
        return localEntries(start) match {
          case fd:FunctionDef => StdFunc.Local(fd.index, resolveApplies(applies, 0, fd.generics.size))
          case id:ImplementDef => ImplFunc.Local(id.index, resolveApplies(applies, 0, id.generics.size))
          case _:DataDef =>
            feedback(LocatedMessage(s"$start does refer to a type but a function is expected", sourceId, Error))
            Func.Unknown
          case _:SignatureDef =>
            feedback(LocatedMessage(s"$start does refer to a type but a function is expected", sourceId, Error))
            Func.Unknown
        }
      }
    }

    //Find all modules matching the path
    val (path, comps) = resolveImport(importPath)
    val entryName = path.last
    val modules = comps.flatMap(_.asModuleInterface)
    val funTargets = modules.flatMap(m => m.functions.filter(_.name == entryName).map((m,_)))
    val implTargets = modules.flatMap(m => m.implements.filter(_.name == entryName).map((m,_)))

    //did we found at least 1
    if(funTargets.isEmpty && implTargets.isEmpty) {
      if(comps.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error))
      } else {
        feedback(LocatedMessage(s"function ${path.mkString(".")} does not exist", sourceId, Error))
      }
      return Func.Unknown
    }

    //did we found more than 1 candidate
    if((funTargets.size + implTargets.size) > 1) feedback(LocatedMessage(s"${path.mkString(".")} is ambiguous (points to multiple candidates)", sourceId, Error))
    //construct the type
    if(funTargets.nonEmpty){
      funTargets.head match {
        case (module,fd) => StdFunc.Remote(module.link, fd.index, resolveApplies(applies, getClassVars(module), fd.generics.size))
      }
    } else if(implTargets.nonEmpty) {
      implTargets.head match {
        case (module,id) => ImplFunc.Remote(module.link, id.index, resolveApplies(applies, getClassVars(module), id.generics.size))
      }
    } else {
      //error was printed during funTargets.isEmpty && implTargets.isEmpty check
      Func.Unknown
    }
  }

  def resolveAliasTarget(importPath:Seq[String], sourceId: SourceId): Option[Instance.EntryRef] = {
    val start = importPath.head
    //If it is just a name not a path it can refer to local declarations
    if(importPath.size == 1) {
      //May be from local module scope
      if(localEntries.contains(start)){
        return localEntries(start) match {
          case fd:FunctionDef => return Some(Instance.LocalEntryRef(fd.index))
          case _:ImplementDef =>
            feedback(LocatedMessage(s"$start does refer to an implement but a function is expected", sourceId, Error))
            return None
          case _:DataDef =>
            feedback(LocatedMessage(s"$start does refer to a type but a function is expected", sourceId, Error))
            return None
          case _:SignatureDef =>
            feedback(LocatedMessage(s"$start does refer to a type but a function expected", sourceId, Error))
            return None
        }
      }
    }

    //Find all modules matching the path
    val (path, comps) = resolveImport(importPath)
    val entryName = path.last
    val modules = comps.flatMap(_.asModuleInterface)
    val funTargets = modules.flatMap(m => m.functions.filter(_.name == entryName).map((m,_)))

    //did we found at least 1
    if(funTargets.isEmpty) {
      if(comps.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error))
      } else {
        feedback(LocatedMessage(s"function ${path.mkString(".")} does not exist", sourceId, Error))
      }
      return None
    }

    //did we found more than 1 candidate
    if(funTargets.size > 1) feedback(LocatedMessage(s"${path.mkString(".")} is ambiguous (points to multiple candidates)", sourceId, Error))

    //construct the type
    if(funTargets.nonEmpty){
      funTargets.head match {
        case (module,fd:FunctionSig) => Some(Instance.RemoteEntryRef(module.link, fd.index))
        case _ => None
      }
    } else {
      None
    }
  }

}
