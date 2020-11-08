package samaya.plugin.impl.inter.json

import samaya.plugin.impl.inter.json.JsonModel.{TypeEncodings, TypeKinds}
import samaya.structure.types.{Accessibility, AdtType, Capability, Hash, LitType, Permission, SigType, Type}
import samaya.structure.{Constructor, DataDef, Field, FunctionSig, Generic, Module, Param, Result, Transaction}

object Serializer {

  def toInterfaceTransactionRepr(txt: Transaction, codeHash:Option[Hash], hasError:Boolean): JsonModel.InterfaceTransaction = {
    val params = txt.params.map(toParamRepr)
    val returns = txt.results.map(toReturnRepr)
    JsonModel.InterfaceTransaction(
      name = txt.name,
      link = codeHash.map(_.toString),
      hadError = hasError,
      language = txt.language,
      version = txt.version,
      classifier = txt.classifier,
      attributes = txt.attributes,
      transactional = txt.transactional,
      params = params,
      returns = returns
    )
  }

  def toInterfaceModuleRepr(module: Module, codeHash:Option[Hash], hasError:Boolean): JsonModel.InterfaceModule = {
    //todo: we once had hidden the local onlies, for functions & implements
    //      but then the default lookup will fail because of missing indexes
    //      if we renable we need to overwrite the default Lookup to use index and not location in array
    val functions = module.functions.map(toFunctionRepr)
    val implements = module.implements.map(toFunctionRepr)
    val sigTypes = module.signatures.map(toFunctionRepr)
    val dataTypes = module.dataTypes.map(toDataTypeRepr)
    JsonModel.InterfaceModule(
      name = module.name,
      link = codeHash.map(_.toString),
      mode = module.mode,
      hadError = hasError,
      language = module.language,
      version = module.version,
      classifier = module.classifier,
      attributes = module.attributes,
      functions = functions,
      implements = implements,
      datatypes = dataTypes,
      sigtypes = sigTypes
    )
  }

  def toFunctionRepr(fun: FunctionSig): JsonModel.FunctionSignature = {
    val generics = fun.generics.map(toGenericRepr)
    val capabilities = fun.capabilities.map(Capability.toString)
    val accessibility = fun.accessibility.map(toAccessRepr)
    val params = fun.params.map(toParamRepr)
    val returns = fun.results.map(toReturnRepr)

    JsonModel.FunctionSignature(
      name = fun.name,
      offset = fun.index,
      position = fun.position,
      attributes = fun.attributes,
      capabilities = capabilities,
      accessibility = accessibility,
      transactional = fun.transactional,
      generics = generics,
      params = params,
      returns = returns
    )
  }

  def toDataTypeRepr(data: DataDef): JsonModel.DataSignature = {
    val capabilities = data.capabilities.map(Capability.toString)
    val accessibility = data.accessibility.map(toAccessRepr)
    val generics = data.generics.map(toGenericRepr)
    val constructors = data.constructors.map(toConstructorRepr)

    JsonModel.DataSignature(
      name = data.name,
      offset = data.index,
      position = data.position,
      attributes = data.attributes,
      accessibility = accessibility,
      capabilities = capabilities,
      generics = generics,
      constructors = constructors,
      external = data.external,
    )
  }

  def toAccessRepr(kv:(Permission, Accessibility)):(String, JsonModel.Accessibility) = {
    kv match {
      case (perm, acc@(Accessibility.Local | Accessibility.Global)) => (perm.toString, JsonModel.Accessibility(Accessibility.toNameString(acc), Set.empty))
      case (perm, acc@Accessibility.Guarded(guards)) => (perm.toString, JsonModel.Accessibility(Accessibility.toNameString(acc), guards))
    }
  }

  def toGenericRepr(generic: Generic): JsonModel.Generic = {
    val capabilities = generic.capabilities.map(Capability.toString)
    JsonModel.Generic(
      name = generic.name,
      attributes = generic.attributes,
      isPhantom = generic.phantom,
      capabilities = capabilities
    )
  }

  def toParamRepr(param: Param): JsonModel.Param = {
    JsonModel.Param(
      name = param.name,
      attributes = param.attributes,
      typ = toTypeRepr(param.typ),
      isConsumed = param.consumes
    )
  }

  def toReturnRepr(result: Result): JsonModel.Return = {
    JsonModel.Return(
      name = result.name,
      attributes = result.attributes,
      typ = toTypeRepr(result.typ)
    )
  }

  def toConstructorRepr(ctr: Constructor): JsonModel.Constructor = {
    val fields = ctr.fields.map(toFieldRepr)
    JsonModel.Constructor(
      name = ctr.name,
      attributes = ctr.attributes,
      fields = fields
    )
  }


  def toFieldRepr(field: Field): JsonModel.Field = {
    JsonModel.Field(
      name = field.name,
      attributes = field.attributes,
      typ = toTypeRepr(field.typ)
    )
  }


  def toTypeRepr(typ: Type): JsonModel.Type = {
    def extractTypeLocation():(String, Int) = {
      typ match {
        case local:Type.LocalLookup[_] => (TypeEncodings.Local.name, local.offset)
        case remote:Type.RemoteLookup[_] => (remote.moduleRef.toString, remote.offset)
      }
    }

    typ match {
      case proj:Type.Projected =>
        JsonModel.Type(
          module = TypeEncodings.Projection.name,
          componentIndex = None,
          applies = Seq(toTypeRepr(proj.inner)),
          attributes = typ.attributes
        )
      case Type.GenericType(_, offset) =>
        JsonModel.Type(
          module = TypeEncodings.Generic.name,
          componentIndex = Some(TypeKinds.Param.name, offset),
          applies = Seq.empty,
          attributes = typ.attributes
        )
      case adt:AdtType =>
        val (module, index) = extractTypeLocation()
        JsonModel.Type(
          module = module,
          componentIndex = Some(TypeKinds.Adt.name, index),
          applies = adt.applies.map(toTypeRepr),
          attributes = typ.attributes
        )
      case lit:LitType =>
        val (module, index) = extractTypeLocation()
        JsonModel.Type(
          module = module,
          componentIndex = Some(TypeKinds.Lit.name, index),
          applies = lit.applies.map(toTypeRepr),
          attributes = typ.attributes
        )
      case sig:SigType =>
        val (module, index) = extractTypeLocation()
        JsonModel.Type(
          module = module,
          componentIndex = Some(TypeKinds.Sig.name, index),
          applies = sig.applies.map(toTypeRepr),
          attributes = typ.attributes
        )
      case _:Type.Unknown =>
        JsonModel.Type(
          module = TypeEncodings.Unknown.name,
          componentIndex = None,
          applies = Seq.empty,
          attributes = typ.attributes
        )
    }
  }
}