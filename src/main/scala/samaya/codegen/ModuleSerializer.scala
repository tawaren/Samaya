package samaya.codegen


import samaya.structure.{CompiledModule, DataDef, FunctionDef, FunctionSig, Generic, ImplementDef, ModuleEntry, Package, SignatureDef, TypeParameterized}
import samaya.structure.types.Hash
import samaya.structure.types._
import samaya.types.Context
import java.io.DataOutputStream

import samaya.compilation.ErrorManager._

import scala.collection.mutable

//todo: compare to sanskrit these may have changed since native elimination (some adaptions already made)
//Serializes a Module interface without the body
object ModuleSerializer {

  val codeExtension = "sans"

  object ImportCollector {
    final val FUN = true
    final val TYP = false
  }

  class ImportCollector(generics:Int, hasSelf:Boolean) {
    private val modOffset:Int = if(hasSelf) 1 else 0
    val modules = new mutable.LinkedHashMap[Hash,Int]()
    val types = new mutable.LinkedHashMap[Type,Int]()
    val funs = new mutable.LinkedHashMap[Func,Int]()
    val permissions = new mutable.LinkedHashMap[(Int,Boolean /*FUN | TYP*/),(Int,mutable.HashSet[Permission])]()

    //todo: asserts for lenghts
    def addModule(link:CompLink):Unit = link match {
      case CompLink.ByCode(hash) => modules.getOrElseUpdate(hash,modules.size+modOffset)
      case CompLink.ByInterface(hash) => feedback(PlainMessage("By interface links are not allowed in finalized modules", Error, CodeGen()))
    } //Note: Module0 is the local thats way +modOffset

    def addPerm(idx:Int, typ:Boolean, perm:Permission):Unit = {
      val set = if(!permissions.contains((idx, typ))){
        val newSet = new mutable.HashSet[Permission]()
        permissions.put((idx, typ), (permissions.size, newSet))
        newSet
      } else {
        permissions((idx, typ))._2
      }
      set.add(perm)
    }

    def addType(typ:Type, perm:Option[Permission] = None): Unit = {
      if(!types.contains(typ)){
        typ match {
          case Type.GenericType(_,_) =>
            //todo: is this already checked elsewhere if not do so and remove
            if(perm.isDefined) feedback(LocatedMessage("A Generic Type can not be referred by a permission", typ.src, Warning, CodeGen()))
            //We do not handle generics as we can get offset from type, no need to enter into map and complicate generic offset logic
            return
          case remote: Type.RemoteLookup[_] => addModule(remote.moduleRef)
          case proj: Type.Projected => addType(proj.inner)
          case _ =>
        }
        typ.applies.foreach(addType(_))
        val idx = types.size+generics
        types.put(typ,idx)
        for(p <- perm) addPerm(idx,ImportCollector.TYP,p)
      } else {
        for(p <- perm) addPerm(types(typ),ImportCollector.TYP,p)
      }
    }

    def addFunction(func:Func, perm:Option[Permission] = None): Unit = {
      if(!funs.contains(func)){
        func match {
          case remote: Func.RemoteLookup[_] => addModule(remote.moduleRef)
          case _ =>
        }
        func.applies.foreach(addType(_))
        val idx = funs.size
        funs.put(func,idx)
        for(p <- perm) addPerm(idx,ImportCollector.FUN,p)
      } else {
        for(p <- perm) addPerm(funs(func),ImportCollector.FUN,p)
      }
    }

    def modIndex(link:CompLink):Byte = link match {
      case CompLink.ByCode(hash) => modules(hash).asInstanceOf[Byte]
      case CompLink.ByInterface(hash) => -1
    }

    def funIndex(func:Func):Byte = funs(func).asInstanceOf[Byte]
    def typeIndex(typ:Type):Byte = typ match {
      case Type.GenericType(_,offset) => offset.asInstanceOf[Byte]
      case _ => types(typ).asInstanceOf[Byte]
    }
    def permIndex(func:Func):Byte = permissions((funIndex(func), ImportCollector.FUN))._1.asInstanceOf[Byte]
    def permIndex(typ:Type):Byte = permissions((typeIndex(typ), ImportCollector.TYP))._1.asInstanceOf[Byte]
  }

  //#[derive(Debug, Parsable, Serializable)]
  //pub struct Module {
  def serialize(out:DataOutputStream, module:CompiledModule, meta:Hash, pkg:Package): Unit = {
    //#[ByteSize]
    //pub byte_size:Option<usize>,
    val context = Context(module,pkg)
    ////A Module has compiler specific meta information (Not of concern to Sanskrit)
    //pub meta: LargeVec<u8>
    out.writeShort(meta.data.length)
    out.write(meta.data,0,meta.data.length)
    ////A Module has Adts
    //pub data: Vec<DataComponent>
    out.writeByte(module.dataTypes.length)
    for((d,i) <- module.dataTypes.zipWithIndex) {
      //todo real error
      assert(d.index == i)
      serializeDataType(out,d)
    }

    ////A Module has Sigs
    //pub sigs: Vec<SigComponent>
    out.writeByte(module.signatures.length)
    for((s,i) <- module.signatures.zipWithIndex) {
      //todo real error
      assert(s.index == i)
      serializeSignatureType(out,s, context)
    }
    ////defines the order of the data components -- true means data, false means sig
    //pub data_sig_order: BitSerializedVec,
    serializeOrderingVec(out, module.dataTypes, module.signatures)

    ////A Module has Functions
    //pub functions: Vec<FunctionComponent>
    out.writeByte(module.functions.length)
    for((f,i) <- module.functions.zipWithIndex) {
      //todo real error
      assert(f.index == i)
      serializeFunction(out, f, context, false)
    }

    ////A Module has Implementations
    //pub implements: Vec<ImplementComponent>
    out.writeByte(module.implements.length)
    for((imp,i) <- module.implements.zipWithIndex) {
      //todo real error
      assert(imp.index == i)
      serializeImplement(out, imp, context)
    }

    ////defines the order of the callable components -- true means function, false means implement
    //pub fun_impl_order: BitSerializedVec
    serializeOrderingVec(out, module.functions, module.implements)
  }
  //}

  def serializeOrderingVec(out: DataOutputStream, trueVec:Seq[ModuleEntry], falseVec:Seq[ModuleEntry]):Unit = {
    var first = trueVec
    var second = falseVec
    val len = trueVec.size + falseVec.size
    out.writeShort(len)
    var curByte = 0
    for(i <- 0 until len) {
      val bit = i % 8
      if(second.isEmpty || (first.nonEmpty && (first.head.position < second.head.position))) {
        first = first.tail
        curByte = curByte | 1 << bit
      } else {
        second = second.tail
      }
      if(bit == 7) {
        out.writeByte(curByte)
        curByte = 0
      }
    }
    if(len % 8 != 0) {
      out.writeByte(curByte)
    }
  }

  //#[derive(Debug, Parsable, Serializable)]
  //pub struct DataComponent {
  def serializeDataType(out: DataOutputStream, dataType:DataDef): Unit ={
    //#[ByteSize]
    //pub byte_size:Option<usize>
    //pub create_scope: Accessibility
    serializeAccessibility(out, dataType.accessibility(Permission.Create), dataType)
    //pub consume_scope: Accessibility
    serializeAccessibility(out, dataType.accessibility(Permission.Consume), dataType)
    //pub inspect_scope: Accessibility
    serializeAccessibility(out, dataType.accessibility(Permission.Inspect), dataType)
    //pub provided_caps:CapSet
    val caps = dataType.capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
    out.writeByte(caps)

    //pub generics:Vec<Generic>
    out.writeByte(dataType.generics.length)
    for(i <- dataType.generics.indices) {
      serializeGeneric(out,dataType.generic(i).get)
    }

    //pub import: PublicImport
    val collector = new ModuleSerializer.ImportCollector(dataType.generics.size, true) //DataTypes are always inside a Module
    dataType.constructors.foreach(ctr => ctr.fields.foreach(field => collector.addType(field.typ)))
    ModuleSerializer.serializeImports(out, collector)

    //pub body:DataImpl
    //  #[derive(Debug, Parsable, Serializable)]
    //  pub enum DataImpl {
    if(dataType.external.isEmpty) {
      //Internal {
      out.writeByte(0)
      //constructors:Vec<Case>
      val ctrs = dataType.constructors.length
      out.writeByte(ctrs)
      for(i <- 0 until ctrs) {
        val ctr = dataType.constructor(i).get
        //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Hash, Debug, Parsable, Serializable)]
        //pub struct Case {
        //  pub fields:Vec<Field>
        val fields = ctr.fields.length
        out.writeByte(fields)
        for(i <- 0 until fields) {
          //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Hash, Debug, Parsable, Serializable)]
          //pub struct Field {
          val field = ctr.field(i).get
          // pub indexed:Vec<u8>     //indexes this is part of
          out.writeByte(0)
          // pub typ:TypeRef
          out.writeByte(collector.typeIndex(field.typ))
        }
        //}
      }
      //}
    } else {
      //External(
      out.writeByte(1)
      //u16
      out.writeShort(dataType.external.get)
      //)
    }
    //}
  }
  //}

  //#[derive(Ord, PartialOrd, Eq, PartialEq, Copy, Clone, Debug, Parsable, Serializable)]
  //pub enum Generic {
  def serializeGeneric(out: DataOutputStream, gen:Generic): Unit ={
    //Phantom
    if(gen.phantom) {
      out.writeByte(0)
    //Physical(
    } else {
      out.writeByte(1)
      //CapSet
      val caps = gen.capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
      out.writeByte(caps)
    }
    //)
  }
  //}


  //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Hash, Debug, Parsable, Serializable)]
  //pub enum Accessibility {
  def serializeAccessibility(out: DataOutputStream, accessibility: Accessibility, applied:TypeParameterized):Unit = {
    accessibility match {
      //Local
      case Accessibility.Local => out.writeByte(0)
      //Guarded(
      case Accessibility.Guarded(guards) =>
        out.writeByte(1)
        //Vec<GenRef>
        out.writeByte(guards.size)
        for(g <- guards) {
          out.writeByte(applied.generics.map(_.name).indexOf(g))
        }
        //)
      //Global
      case Accessibility.Global => out.writeByte(2)
    }
  }
  //}

  //#[derive(Debug, Parsable, Serializable)]
  def serializeSignatureType(out: DataOutputStream,sig:SignatureDef, context:Context): Unit ={
    //pub struct SigComponent {
    //    #[ByteSize]
    //    pub byte_size:Option<usize>
    //    pub call_scope: Accessibility
    serializeAccessibility(out, sig.accessibility(Permission.Call), sig)
    //    pub implement_scope: Accessibility
    serializeAccessibility(out, sig.accessibility(Permission.Define), sig)
    //    pub provided_caps:CapSet
    val caps = sig.capabilities.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
    out.writeByte(caps)
    //    pub shared:FunSigShared
    serializeFunSigShared(out,sig,context, None, false)
  }
  //}


  //#[derive(Debug, Parsable, Serializable)]
  //pub struct FunctionComponent {
  def serializeFunction(out: DataOutputStream, function:FunctionDef, context:Context, isTransaction:Boolean): Unit ={
    //    #[ByteSize]
    //    pub byte_size:Option<usize>,
    //    pub scope: Accessibility,
    serializeAccessibility(out, function.accessibility(Permission.Call), function)
    //    pub shared:FunSigShared,
    val collector = serializeFunSigShared(out,function,context, Some(Left(function)), isTransaction)
    //    pub body: CallableImpl
    //      #[derive(Debug, Parsable, Serializable)]
    //      pub enum CallableImpl {
    if(function.external) {
      //      External
      out.writeByte(0)
    } else {
      //      Internal{
      out.writeByte(1)
      //        #[ByteSize]
      //        byte_size:Option<usize>,
      //        imports:BodyImport,
      ModuleSerializer.serializeFunImports(out, collector)
      //        code: Exp
      CodeSerializer.serializeCode(out,collector,Left(function), context)
      //      }
    }
    //}
  }
  //}

  //#[derive(Debug, Parsable, Serializable)]
  //pub struct ImplementComponent {
  def serializeImplement(out: DataOutputStream, implement:ImplementDef, context:Context): Unit = {
    //    #[ByteSize]
    //    pub byte_size:Option<usize>,
    //    pub scope: Accessibility
    serializeAccessibility(out, implement.accessibility(Permission.Call), implement)
    //    pub sig: PermRef
    val collector = new ModuleSerializer.ImportCollector(implement.generics.size, true) //Implements are always inside a Module
    implement.params.foreach(param => collector.addType(param.typ))
    implement.results.foreach(result => collector.addType(result.typ))
    collector.addType(implement.implements, Some(Permission.Define))
    CodeSerializer.collectFunctionBodyDependencies(collector, Right(implement), context)
    out.writeByte(collector.permIndex(implement.implements:Type))
    //    pub generics:Vec<Generic>
    out.writeByte(implement.generics.length)
    for(i <- implement.generics.indices) {
      serializeGeneric(out,implement.generic(i).get)
    }
    //    pub import: PublicImport
    ModuleSerializer.serializeImports(out, collector)
    //    pub params:Vec<Param>
    out.writeByte(implement.params.size)
    for(p <- implement.params) {
      //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Copy, Hash, Debug, Parsable, Serializable)]
      //pub struct Param{
      //    pub consumes:bool
      out.writeBoolean(p.consumes)
      //    pub typ:TypeRef
      out.writeByte(collector.typeIndex(p.typ))
      //}
    }
    //    pub body: CallableImpl
    //      #[derive(Debug, Parsable, Serializable)]
    //      pub enum CallableImpl {
    if(implement.external) {
      //      External
      out.writeByte(0)
    } else {
      //      Internal{
      out.writeByte(1)
      //        #[ByteSize]
      //        byte_size:Option<usize>,
      //        imports:BodyImport,
      ModuleSerializer.serializeFunImports(out, collector)
      //        code: Exp
      CodeSerializer.serializeCode(out,collector,Right(implement), context)
      //      }
    }
  }
  //}

  //#[derive(Debug, Parsable, Serializable)]
  //pub struct FunSigShared {
  def serializeFunSigShared(out: DataOutputStream, funSig:FunctionSig, context: Context, body:Option[Either[FunctionDef,ImplementDef]], isTransaction: Boolean):ImportCollector = {
    //    pub transactional:bool
    out.writeBoolean(funSig.transactional)
    //    pub generics:Vec<Generic>
    out.writeByte(funSig.generics.length)
    for(i <- funSig.generics.indices) {
      serializeGeneric(out,funSig.generic(i).get)
    }
    //    pub import: PublicImport
    val collector = new ModuleSerializer.ImportCollector(funSig.generics.size, !isTransaction)
    funSig.params.foreach(param => collector.addType(param.typ))
    funSig.results.foreach(result => collector.addType(result.typ))
    body.foreach(CodeSerializer.collectFunctionBodyDependencies(collector, _, context))
    ModuleSerializer.serializeImports(out, collector)
    //    pub params:Vec<Param>
    out.writeByte(funSig.params.size)
    for(p <- funSig.params) {
      //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Copy, Hash, Debug, Parsable, Serializable)]
      //pub struct Param{
      //    pub consumes:bool
      out.writeBoolean(p.consumes)
      //    pub typ:TypeRef
      out.writeByte(collector.typeIndex(p.typ))
      //}
    }
    //    pub returns:Vec<TypeRef>,                   //A Fun/Sig has returns
    out.writeByte(funSig.results.size)
    for(r <- funSig.results) {
      out.writeByte(collector.typeIndex(r.typ))
    }
    collector
  }
  //}

  //#[derive(Debug, Parsable, Serializable)]
  //pub struct PublicImport {
  def serializeImports(out:DataOutputStream, imports:ImportCollector): Unit = {
    //    pub modules:Vec<ModuleLink>
    out.writeByte(imports.modules.size)
    imports.modules.foreach( hp => out.write(hp._1.data))
    //    pub types:Vec<TypeImport>
    out.writeByte(imports.types.size)
    //#[derive(Ord, PartialOrd, Eq, PartialEq, Hash, Debug, Parsable, Serializable)]
    //pub enum TypeImport {
    imports.types.keys.foreach({
      //Projection{
      case proj: Type.Projected =>
        out.writeByte(0)
        //typ:TypeRef
        out.writeByte(imports.typeIndex(proj.inner))
      //}
      //Sig{
      case sig:SigType =>
        out.writeByte(1)
        //link:SigLink
        // #[derive(Ord, PartialOrd, Eq, PartialEq, Hash, Copy, Clone, Debug, Parsable, Serializable)]
        // pub struct SigLink{
        val (mod,off) =sig match {
          case SigType.Local(offset, _) => (0.toByte,offset)
          case SigType.Remote(moduleHash, offset, _)  => (imports.modIndex(moduleHash), offset)
        }
        //  pub module:ModRef
        out.writeByte(mod)
        //  pub offset:u8
        out.writeByte(off)
        // }
        //applies:Vec<TypeRef>
        out.writeByte(sig.applies.length)
        for(t <- sig.applies) {
          out.writeByte(imports.typeIndex(t))
        }
      //}
      //Data{
      case data: DataType =>
        out.writeByte(2)
        //link:DataLink
        // #[derive(Ord, PartialOrd, Eq, PartialEq, Hash, Copy, Clone, Debug, Parsable, Serializable)]
        // pub struct DataLink{
        val (mod,off) = data match {
          case AdtType.Local(offset, _) => (0.toByte, offset)
          case LitType.Local(offset, _) => (0.toByte, offset)
          case AdtType.Remote(moduleHash, offset, _) => (imports.modIndex(moduleHash), offset)
          case LitType.Remote(moduleHash, offset, _) => (imports.modIndex(moduleHash), offset)
            //Please compiler
          case _: Type.Projected => unexpected("should never happen", CodeGen())
        }
        //  pub module:ModRef
        out.writeByte(mod)
        //  pub offset:u8
        out.writeByte(off)
        // }
        //applies:Vec<TypeRef>}
        out.writeByte(data.applies.length)
        for(t <- data.applies) {
          out.writeByte(imports.typeIndex(t))
        }
      //}
      case _:Type.Unknown => unexpected("Unknown type reached Serializer", CodeGen())
    })
    //}
  }
  //}

  //#[derive(Debug, Parsable, Serializable)]
  //pub struct BodyImport {
  def serializeFunImports(out:DataOutputStream, imports:ImportCollector): Unit = {
    //    pub public: PublicImport
    out.writeByte(0)  //We import all publics globally (anything else would need a better collector)
    out.writeByte(0)  //We import all publics globally (anything else would need a better collector)
    //    pub callables:Vec<CallableImport>
    out.writeByte(imports.funs.size)
    imports.funs.keys.foreach( fun => {
      //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Hash, Debug, Parsable, Serializable)]
      //pub enum CallableImport {
      val (caseIdx, mod, off) = fun match {
        // Function{
        //link:FuncLink
        case StdFunc.Local(offset, _) => (0.toByte, 0.toByte, offset)
        case StdFunc.Remote(moduleHash, offset, _) => (0.toByte, imports.modIndex(moduleHash), offset)
        // Implement{
        // link:ImplLink
        case ImplFunc.Local(offset, _) => (1.toByte, 0.toByte, offset)
        case ImplFunc.Remote(moduleHash, offset, _) => (1.toByte, imports.modIndex(moduleHash), offset)

        case _:Func.Unknown => unexpected("Unknown Function Reached Serializer", CodeGen())
      }
      out.writeByte(caseIdx)
      //  pub module:ModRef
      out.writeByte(mod)
      //  pub offset:u8
      out.writeByte(off)
      // }
      // applies:Vec<TypeRef>
      out.writeByte(fun.applies.length)
      for(t <- fun.applies) {
        out.writeByte(imports.typeIndex(t))
      }
      //}
    })
    //    pub permissions:Vec<PermissionImport>
    out.writeByte(imports.permissions.size)
    //#[derive(Ord, PartialOrd, Eq, PartialEq, Clone, Hash, Debug, Parsable, Serializable)]
    //pub enum PermissionImport {
    imports.permissions.foreach({
      //Type(
      case ((ref, ImportCollector.TYP),(_, perms)) =>
        out.writeByte(0)
        //PermSet
        val permSet = perms.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
        out.writeByte(permSet)
        //TypeRef
        out.writeByte(ref)
      //)
      //Callable(
      case ((ref, ImportCollector.FUN),(_, perms)) =>
        out.writeByte(1)
        //PermSet
        val permSet = perms.map(c => c.mask).fold(0.asInstanceOf[Byte]){ (a, m) => (a|m).asInstanceOf[Byte]}
        out.writeByte(permSet)
        //CallRef
        out.writeByte(ref)
      //)
    })
    //}
  }
  //}
}
