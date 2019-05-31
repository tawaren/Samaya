package mandalac.plugin.impl.inter.json

import mandalac.plugin.impl.inter.json.JsonModel.TypeEncodings
import mandalac.structure.types.{Capability, Type}
import mandalac.structure.{Constructor, DataType, Field, FunctionDef, Generic, Module, Param, Result, Risk, types}

object Serializer {

  def toInterfaceRepr(module: Module): JsonModel.Interface = {
    val functions = module.functions.map(toFunctionRepr)
    val datatypes = module.dataTypes.map(toDataTypeRepr)
    val risks = module.risks.map(toRiskRepr)
    JsonModel.Interface(
      name = module.name,
      hash = module.hash.toString,
      language = module.language,
      version = module.version,
      classifier = module.classifier,
      functions = functions,
      datatypes = datatypes,
      risks = risks
    )
  }

  def toFunctionRepr(fun: FunctionDef): JsonModel.Function = {
    val risks = fun.risks.map(toRefRepr)
    val generics = fun.generics.map(toGenericRepr)
    val params = fun.params.map(toParamRepr)
    val returns = fun.results.map(toReturnRepr)
    JsonModel.Function(
      name = fun.name,
      offset = fun.index,
      risks = risks,
      generics = generics,
      params = params,
      returns = returns
    )
  }

  def toDataTypeRepr(data: DataType): JsonModel.Datatype = {
    val capabilities = data.capabilities.map(Capability.toString)
    val generics = data.generics.map(toGenericRepr)
    val constructors = data.constructors.map(toConstructorRepr)
    JsonModel.Datatype(
      name = data.name,
      offset = data.index,
      capabilities = capabilities,
      generics = generics,
      constructors = constructors
    )
  }

  def toRiskRepr(risk: Risk): JsonModel.Risk = JsonModel.Risk(name = risk.name, offset = risk.index)

  def toRefRepr(risk: types.Risk): JsonModel.Ref = {
    risk match {
      case types.Risk.Local(offset) => JsonModel.Ref(module = TypeEncodings.Local.name, offset = Some(offset))
      case types.Risk.Module(module, offset) =>  JsonModel.Ref(module = module.toString, offset = Some(offset))
      case types.Risk.Native(kind) =>  JsonModel.Ref(offset = Some(kind.ident))
    }
  }

  def toGenericRepr(generic: Generic): JsonModel.Generic = {
    val capabilities = generic.capabilities.map(Capability.toString)
    JsonModel.Generic(
      name = generic.name,
      isProtected = generic.protection,
      isPhantom = generic.phantom,
      capabilities = capabilities
    )
  }

  def toParamRepr(param: Param): JsonModel.Param = {
    JsonModel.Param(
      name = param.name,
      typ = toTypeRepr(param.typ),
      isConsumed = param.consumes
    )
  }

  def toReturnRepr(result: Result): JsonModel.Return = {
    JsonModel.Return(
      name = result.name,
      typ = toTypeRepr(result.typ),
      borrows = result.borrows
    )
  }

  def toConstructorRepr(ctr: Constructor): JsonModel.Constructor = {
    val fields = ctr.fields.map(toFieldRepr)
    JsonModel.Constructor(
      name = ctr.name,
      fields = fields
    )
  }

  def toTypeRepr(typ: Type): JsonModel.Type = {
    typ match {
      case Type.LocalType(offset, applies) =>
        JsonModel.Type(
          JsonModel.Ref(module = TypeEncodings.Local.name, offset = Some(offset)),
          applies = applies.map(toTypeRepr)
        )
      case Type.RealType(moduleHash, offset, applies) =>
        JsonModel.Type(
          JsonModel.Ref(module = moduleHash.toString, offset = Some(offset)),
          applies = applies.map(toTypeRepr)
        )
      case Type.NativeType(t, applies) =>
        JsonModel.Type(
          JsonModel.Ref(
            module = TypeEncodings.Native.name,
            offset = Some(t.ident),
            args = Some(t.arg)
          ),
          applies = applies.map(toTypeRepr)
        )
      case Type.ImageType(t) =>
        JsonModel.Type(
          JsonModel.Ref(
            module = TypeEncodings.Image.name
          ),
          applies = Seq(toTypeRepr(t)))
      case Type.GenericType(_, offset) =>
        JsonModel.Type(
          JsonModel.Ref(
            module = TypeEncodings.Generic.name,
            offset = Some(offset)
          ),
          applies = Seq.empty
        )
    }
  }

  def toFieldRepr(field: Field): JsonModel.Field = {
    JsonModel.Field(
      name = field.name,
      typ = toTypeRepr(field.typ)
    )
  }

}