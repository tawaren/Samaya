package samaya.plugin.impl.compiler.mandala.compiler

import samaya.compilation.ErrorManager.{Error, LocatedMessage, Warning, feedback}
import samaya.plugin.impl.compiler.mandala.{MandalaCompiler, MandalaParser}
import samaya.plugin.impl.compiler.mandala.components.clazz.Class
import samaya.plugin.impl.compiler.mandala.components.instance.{DefInstance, Instance}
import samaya.plugin.impl.compiler.mandala.components.module.MandalaModule
import samaya.plugin.impl.compiler.mandala.entry.TypeAlias
import samaya.structure.types.{AdtType, Capability, DefinedType, Func, ImplFunc, LitType, SigType, SourceId, StdFunc, Type}
import samaya.structure.{Attribute, Component, DataDef, FunctionDef, Generic, ImplementDef, Interface, Module, Package, SignatureDef}
import samaya.toolbox.process.TypeInference

import scala.collection.JavaConverters._

trait ComponentResolver extends CompilerToolbox {
  self: ComponentBuilder with CapabilityCompiler =>

  var imports: Map[String, Seq[String]] = Map[String,Seq[String]]()

  //This is per component
  private var availableGenerics = Map.empty[String,Generic]
  private var _componentGenerics:Seq[Generic] = Seq.empty

  def withGenerics[T](generics:Seq[Generic])(body: => T):T={
    assert(generics.zipWithIndex.forall(gi => gi._1.index == gi._2))
    assert(generics.startsWith(_componentGenerics))

    val oldGenerics =  availableGenerics
    availableGenerics = generics.map(g => (g.name,g)).toMap
    val res = body
    availableGenerics = oldGenerics
    res
  }

  def componentGenerics:Seq[Generic] = _componentGenerics
  def withComponentGenerics[T](generics:Seq[Generic])(body: => T):T = {
    val oldComponentGenerics = _componentGenerics
    _componentGenerics = generics
    val res = body
    _componentGenerics = oldComponentGenerics
    res
  }

  override def visitGenericArgs(ctx: MandalaParser.GenericArgsContext): Seq[Generic] = {
    //this spares the caller the check and simplifies the code
    if(ctx == null) return _componentGenerics
    _componentGenerics ++ withFreshIndex({
      ctx.generics.asScala.map(visitGenericDef)
    })
  }

  override def visitGenericDef(ctx: MandalaParser.GenericDefContext): Generic = new Generic {
    override val name: String = visitName(ctx.name)
    override val index: Int = nextIndex() + _componentGenerics.length
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
      feedback(LocatedMessage(s"Wildcard import ${elems.mkString(".")} does not point to a component", sourceId, Warning))
      return
    }
    for(module <- components) {
      for(c <- module.functions ++ module.dataTypes ++ module.signatures ++ module.implements) {
        recordImport(elems :+ c.name, None, sourceId)
      }
      module match {
        case mm:MandalaModule => for(c <- mm.typeAlias){
          recordImport(elems :+ c.name, None, sourceId)
        }
        case _ =>
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

  private def addImport(elems:Seq[String], rename:Option[String], sourceId: SourceId): Unit ={
    recordImport(elems, rename, sourceId)
    val mod = elems.last
    val path = elems.init

    env.pkg.componentByPathAndName(path,mod).foreach {
      case defInstance:DefInstance =>
        env.instRec(defInstance, isLocal = false)
      case _ =>
    }

    if(path.nonEmpty) {
      env.pkg.componentByPathAndName(path.init,Instance.deriveTopName(path.last,mod)).foreach{
        case defInstance:DefInstance => env.instRec(defInstance, isLocal = false)
        case _ =>
      }
    }

  }

  private def recordImport(elems:Seq[String], rename:Option[String], sourceId: SourceId): Unit ={
    val last = rename.getOrElse(elems.last)
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
    }  else {
      val rename = if(ctx.name() != null) {
        Some(visitName(ctx.name()))
      } else {
        None
      }
      addImport(elems, rename, src)
    }
  }


  override def visitTypeRefArgs(ctx: MandalaParser.TypeRefArgsContext): Seq[Type] = {
    ctx.targs.asScala.map(visitTypeRef)
  }

  private def extractBaseRef(ctx: MandalaParser.BaseRefContext): (Seq[String],Option[Seq[Type]], Option[Seq[Type]]) = {
    var parts = ctx.path().part.asScala.map(visitName)
    val applies = if(ctx.targs == null) None else Some(ctx.targs.targs.asScala.map(visitTypeRef))
    val compApplies = if(ctx.compArgs != null){
      parts = parts :+ visitName(ctx.name())
      Some(ctx.compArgs.targs.asScala.map(visitTypeRef))
    } else {
      None
    }
    (parts, compApplies, applies)
  }

  override def visitBaseRef(ctx: MandalaParser.BaseRefContext): Type = {
    val (parts, compApplies, applies) = extractBaseRef(ctx)
    //Resolve arguments
    resolveType(parts, compApplies, applies, sourceIdFromContext(ctx))
  }

  def visitFunRef(ctx: MandalaParser.BaseRefContext, numArgs:Option[Int]): Func = {
    val (parts, compApplies, applies) = extractBaseRef(ctx);
    //Resolve arguments
    resolveFunc(parts, compApplies, applies, numArgs, sourceIdFromContext(ctx))  }

  override def visitTypeRef(ctx: MandalaParser.TypeRefContext): Type = {
    val src = sourceIdFromContext(ctx)
    if(ctx == null) {
      TypeInference.TypeVar(src)
    }else if(ctx.PROJECT() != null) {
      visitTypeRef(ctx.typeRef()).projected(src)
    } else if(ctx.QUEST() != null) {
      TypeInference.TypeVar(src)
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

  private def resolveApplies(compApplies:Option[Seq[Type]], applies:Option[Seq[Type]], classVars:Int, expected:Int, src:SourceId):Seq[Type] = {
    assert(classVars <= expected)
    val componentApplies = compApplies match {
      case None => Seq.fill(classVars)(TypeInference.TypeVar(src));
      case Some(value) => value
    }

    applies match {
      case Some(value) => componentApplies ++ value
      case None => componentApplies ++ Seq.fill(expected-classVars)(TypeInference.TypeVar(src))
    }
  }
  //todo: we need some unified dependency lookup model
  //The resolver algorithms
  def resolveType(importPath:Seq[String],compApplies:Option[Seq[Type]], applies:Option[Seq[Type]], sourceId:SourceId): Type = {
    val start = importPath.head
    //If it is just a name not a path it can refer to local declarations
    if(importPath.size == 1) {
      //May be from local scope
      if(availableGenerics.contains(start)){
        //check that there are no params
        if(applies.nonEmpty) feedback(LocatedMessage(s"Generics do not have parameters", sourceId, Error))
        return availableGenerics(start).asType(sourceId)
      }

      //May be from local module scope
      if(localEntries.contains(start)){
        return localEntries(start) match {
          case dt:DataDef if dt.external.isDefined => LitType.Local(dt.index, resolveApplies(compApplies, applies, 0, dt.generics.size, sourceId))(sourceId)
          case dt:DataDef if dt.external.isEmpty => AdtType.Local(dt.index, resolveApplies(compApplies, applies, 0, dt.generics.size, sourceId))(sourceId)
          case st:SignatureDef => SigType.Local(st.index, resolveApplies(compApplies, applies, 0, st.generics.size, sourceId))(sourceId)
          case _:FunctionDef =>
            feedback(LocatedMessage(s"$start does refer to a function but a type is expected", sourceId, Error))
            Type.Unknown(Set.empty)(sourceId)
          case _:ImplementDef =>
            feedback(LocatedMessage(s"$start does refer to an implement but a type is expected", sourceId, Error))
            Type.Unknown(Set.empty)(sourceId)
        }
      }

      //Maybe a local Alias
      localAliases.find(ta => ta.name == start) match {
        case Some(TypeAlias(_,gens,typ,_)) =>
          return typ.instantiate(resolveApplies(compApplies, applies, 0, gens.size, sourceId))
        case _ =>
      }
    }
    val (path, comps) = resolveImport(importPath)
    val entryName = path.last
    val modules = comps.flatMap(_.asModuleInterface);
    val typeTargets = modules.flatMap(m => (m.dataTypes ++ m.signatures).filter(_.name == entryName).map((m,_)))
    val aliases = modules.flatMap{
      case md:MandalaModule with Interface[_] => md.typeAlias.filter(ta => ta.name == entryName).map((md,_))
      case _ => Seq.empty
    }
    //did we found at least 1
    if(typeTargets.isEmpty && aliases.isEmpty) {
      if(comps.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error))
      } else {
        feedback(LocatedMessage(s"typ ${path.mkString(".")} does not exist", sourceId, Error))
      }
      return Type.Unknown(Set.empty)(sourceId)
    }

    //did we found more than 1 candidate
    if(typeTargets.size + aliases.size > 1) {
      feedback(LocatedMessage(s"${path.mkString(".")} is ambiguous (points to multiple candidates)", sourceId, Error))
      return Type.Unknown(Set.empty)(sourceId)
    }
    //construct the type
    //Note: currently classes can not define DataTypes but we still add the getClassVars(module) as we may allow it in the future
    if(typeTargets.nonEmpty){
      typeTargets.map{
        case (module,dt:DataDef) if dt.external.isDefined => LitType.Remote(module.link, dt.index, resolveApplies(compApplies, applies, getClassVars(module), dt.generics.size, sourceId))(sourceId)
        case (module,dt:DataDef) if dt.external.isEmpty => AdtType.Remote(module.link, dt.index, resolveApplies(compApplies, applies, getClassVars(module), dt.generics.size, sourceId))(sourceId)
        case (module,st:SignatureDef) => SigType.Remote(module.link, st.index, resolveApplies(compApplies, applies, getClassVars(module), st.generics.size, sourceId))(sourceId)
        case _ => Type.Unknown(Set.empty)(sourceId)
      }.head
    } else  {
      aliases.map{
        case (md, TypeAlias(_,gens,typ,_)) =>
          val glType = Type.globalizeLocals(md.link, typ)
          val attrs = glType match {
            case df:DefinedType[_] => df.getComponent(context).flatMap(_.asModuleInterface) match {
                case Some(mod) =>
                  val modAttr = Attribute(MandalaCompiler.Aliasing_Module_Attribute_Name, Attribute.Text(mod.link.toString))
                  typ.attributes.filterNot(_ == modAttr)
                case _ => typ.attributes
              }
            case _ => typ.attributes
          }
          val newAttributes = attrs :+ Attribute(MandalaCompiler.Aliasing_Module_Attribute_Name, Attribute.Text(md.link.toString))
          glType.instantiate(resolveApplies(compApplies, applies, 0, gens.size, sourceId)).changeMeta(attributes = newAttributes)
      }.head
    }
  }

  private def getClassVars(module:Module) = module match {
    case cls:Class => cls.generics.size
    case _ => 0
  }

  def resolveFunc(importPath:Seq[String],compApplies:Option[Seq[Type]], applies:Option[Seq[Type]], numArgs:Option[Int], sourceId: SourceId): Func = {
    val start = importPath.head
    //If it is just a name not a path it can refer to local declarations
    if(importPath.size == 1) {
      def resolveLocalEntry(name:String):Func = {
        localEntries(name) match {
          case fd:FunctionDef => StdFunc.Local(fd.index, resolveApplies(compApplies, applies, 0, fd.generics.size, sourceId))(sourceId)
          case id:ImplementDef => ImplFunc.Local(id.index, resolveApplies(compApplies,applies, 0, id.generics.size, sourceId))(sourceId)
          case _:DataDef =>
            feedback(LocatedMessage(s"$start does refer to a type but a function is expected", sourceId, Error))
            Func.Unknown(sourceId)
          case _:SignatureDef =>
            feedback(LocatedMessage(s"$start does refer to a type but a function is expected", sourceId, Error))
            Func.Unknown(sourceId)
        }
      }
      //May be from local module scope
      if(localEntries.contains(start)) return resolveLocalEntry(start)
      //May be local & overloaded??
      if(numArgs.nonEmpty) {
        val overloadName = MandalaModule.deriveOverloadedName(start, numArgs.get)
        if(localEntries.contains(overloadName)) return resolveLocalEntry(overloadName)
      }

    }

    //Find all modules matching the path
    val (path, comps) = resolveImport(importPath)
    val names = if(numArgs.nonEmpty){
      val entryName = path.last
      val overloadEntryName = MandalaModule.deriveOverloadedName(entryName,numArgs.get)
      Set(entryName,overloadEntryName)
    } else {
      Set(path.last)
    }
    val modules = comps.flatMap(_.asModuleInterface)
    val funTargets = modules.flatMap(m => m.functions.filter(n => names.contains(n.name)).map((m,_)))
    val implTargets = modules.flatMap(m => m.implements.filter(n => names.contains(n.name)).map((m,_)))

    //did we found at least 1
    if(funTargets.isEmpty && implTargets.isEmpty) {
      if(comps.isEmpty && path.init.nonEmpty) {
        feedback(LocatedMessage(s"Component ${path.init.mkString(".")} is missing in workspace", sourceId, Error))
      } else {
        feedback(LocatedMessage(s"Function ${path.mkString(".")} does not exist", sourceId, Error))
      }
      return Func.Unknown(sourceId)
    }

    //did we found more than 1 candidate
    if((funTargets.size + implTargets.size) > 1) feedback(LocatedMessage(s"${path.mkString(".")} is ambiguous (points to multiple candidates)", sourceId, Error))
    //construct the type
    if(funTargets.nonEmpty){
      funTargets.head match {
        case (module,fd) => StdFunc.Remote(module.link, fd.index, resolveApplies(compApplies, applies, getClassVars(module), fd.generics.size, sourceId))(sourceId)
      }
    } else if(implTargets.nonEmpty) {
      implTargets.head match {
        case (module,id) => ImplFunc.Remote(module.link, id.index, resolveApplies(compApplies, applies, getClassVars(module), id.generics.size, sourceId))(sourceId)
      }
    } else {
      //error was printed during funTargets.isEmpty && implTargets.isEmpty check
      Func.Unknown(sourceId)
    }
  }
}
