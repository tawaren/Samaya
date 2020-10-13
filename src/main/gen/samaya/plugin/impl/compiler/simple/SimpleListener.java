// Generated from C:/Users/Markus Knecht/Dropbox/Privat/UZH/PhD/code/Samaya/src/main/antlr/samaya/plugin/impl/compiler/simple\Simple.g4 by ANTLR 4.7.2
package samaya.plugin.impl.compiler.simple;

    package samaya.plugin.impl.compiler.simple;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link SimpleParser}.
 */
public interface SimpleListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link SimpleParser#file}.
	 * @param ctx the parse tree
	 */
	void enterFile(SimpleParser.FileContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#file}.
	 * @param ctx the parse tree
	 */
	void exitFile(SimpleParser.FileContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#import_}.
	 * @param ctx the parse tree
	 */
	void enterImport_(SimpleParser.Import_Context ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#import_}.
	 * @param ctx the parse tree
	 */
	void exitImport_(SimpleParser.Import_Context ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void enterWildcard(SimpleParser.WildcardContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#wildcard}.
	 * @param ctx the parse tree
	 */
	void exitWildcard(SimpleParser.WildcardContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#module}.
	 * @param ctx the parse tree
	 */
	void enterModule(SimpleParser.ModuleContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#module}.
	 * @param ctx the parse tree
	 */
	void exitModule(SimpleParser.ModuleContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#component}.
	 * @param ctx the parse tree
	 */
	void enterComponent(SimpleParser.ComponentContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#component}.
	 * @param ctx the parse tree
	 */
	void exitComponent(SimpleParser.ComponentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Data}
	 * labeled alternative in {@link SimpleParser#dataDef}.
	 * @param ctx the parse tree
	 */
	void enterData(SimpleParser.DataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Data}
	 * labeled alternative in {@link SimpleParser#dataDef}.
	 * @param ctx the parse tree
	 */
	void exitData(SimpleParser.DataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Function}
	 * labeled alternative in {@link SimpleParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void enterFunction(SimpleParser.FunctionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link SimpleParser#functionDef}.
	 * @param ctx the parse tree
	 */
	void exitFunction(SimpleParser.FunctionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Error}
	 * labeled alternative in {@link SimpleParser#errorDef}.
	 * @param ctx the parse tree
	 */
	void enterError(SimpleParser.ErrorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Error}
	 * labeled alternative in {@link SimpleParser#errorDef}.
	 * @param ctx the parse tree
	 */
	void exitError(SimpleParser.ErrorContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#ext}.
	 * @param ctx the parse tree
	 */
	void enterExt(SimpleParser.ExtContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#ext}.
	 * @param ctx the parse tree
	 */
	void exitExt(SimpleParser.ExtContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#litSize}.
	 * @param ctx the parse tree
	 */
	void enterLitSize(SimpleParser.LitSizeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#litSize}.
	 * @param ctx the parse tree
	 */
	void exitLitSize(SimpleParser.LitSizeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#ctrs}.
	 * @param ctx the parse tree
	 */
	void enterCtrs(SimpleParser.CtrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#ctrs}.
	 * @param ctx the parse tree
	 */
	void exitCtrs(SimpleParser.CtrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#ctr}.
	 * @param ctx the parse tree
	 */
	void enterCtr(SimpleParser.CtrContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#ctr}.
	 * @param ctx the parse tree
	 */
	void exitCtr(SimpleParser.CtrContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#fields}.
	 * @param ctx the parse tree
	 */
	void enterFields(SimpleParser.FieldsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#fields}.
	 * @param ctx the parse tree
	 */
	void exitFields(SimpleParser.FieldsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#field}.
	 * @param ctx the parse tree
	 */
	void enterField(SimpleParser.FieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#field}.
	 * @param ctx the parse tree
	 */
	void exitField(SimpleParser.FieldContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Public}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 */
	void enterPublic(SimpleParser.PublicContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Public}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 */
	void exitPublic(SimpleParser.PublicContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Private}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 */
	void enterPrivate(SimpleParser.PrivateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Private}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 */
	void exitPrivate(SimpleParser.PrivateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Protected}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 */
	void enterProtected(SimpleParser.ProtectedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Protected}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 */
	void exitProtected(SimpleParser.ProtectedContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#risky}.
	 * @param ctx the parse tree
	 */
	void enterRisky(SimpleParser.RiskyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#risky}.
	 * @param ctx the parse tree
	 */
	void exitRisky(SimpleParser.RiskyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#protection}.
	 * @param ctx the parse tree
	 */
	void enterProtection(SimpleParser.ProtectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#protection}.
	 * @param ctx the parse tree
	 */
	void exitProtection(SimpleParser.ProtectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#params}.
	 * @param ctx the parse tree
	 */
	void enterParams(SimpleParser.ParamsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#params}.
	 * @param ctx the parse tree
	 */
	void exitParams(SimpleParser.ParamsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#param}.
	 * @param ctx the parse tree
	 */
	void enterParam(SimpleParser.ParamContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#param}.
	 * @param ctx the parse tree
	 */
	void exitParam(SimpleParser.ParamContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#rets}.
	 * @param ctx the parse tree
	 */
	void enterRets(SimpleParser.RetsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#rets}.
	 * @param ctx the parse tree
	 */
	void exitRets(SimpleParser.RetsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#ret}.
	 * @param ctx the parse tree
	 */
	void enterRet(SimpleParser.RetContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#ret}.
	 * @param ctx the parse tree
	 */
	void exitRet(SimpleParser.RetContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#borrows}.
	 * @param ctx the parse tree
	 */
	void enterBorrows(SimpleParser.BorrowsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#borrows}.
	 * @param ctx the parse tree
	 */
	void exitBorrows(SimpleParser.BorrowsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#funBody}.
	 * @param ctx the parse tree
	 */
	void enterFunBody(SimpleParser.FunBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#funBody}.
	 * @param ctx the parse tree
	 */
	void exitFunBody(SimpleParser.FunBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#code}.
	 * @param ctx the parse tree
	 */
	void enterCode(SimpleParser.CodeContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#code}.
	 * @param ctx the parse tree
	 */
	void exitCode(SimpleParser.CodeContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#r_return}.
	 * @param ctx the parse tree
	 */
	void enterR_return(SimpleParser.R_returnContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#r_return}.
	 * @param ctx the parse tree
	 */
	void exitR_return(SimpleParser.R_returnContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#r_throw}.
	 * @param ctx the parse tree
	 */
	void enterR_throw(SimpleParser.R_throwContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#r_throw}.
	 * @param ctx the parse tree
	 */
	void exitR_throw(SimpleParser.R_throwContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Pack}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterPack(SimpleParser.PackContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Pack}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitPack(SimpleParser.PackContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Literal}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(SimpleParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Literal}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(SimpleParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Invoke}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterInvoke(SimpleParser.InvokeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Invoke}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitInvoke(SimpleParser.InvokeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Get}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterGet(SimpleParser.GetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Get}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitGet(SimpleParser.GetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Fetch}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterFetch(SimpleParser.FetchContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Fetch}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitFetch(SimpleParser.FetchContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Unpack}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterUnpack(SimpleParser.UnpackContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Unpack}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitUnpack(SimpleParser.UnpackContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Image}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterImage(SimpleParser.ImageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Image}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitImage(SimpleParser.ImageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ExtractImage}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterExtractImage(SimpleParser.ExtractImageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ExtractImage}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitExtractImage(SimpleParser.ExtractImageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Let}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterLet(SimpleParser.LetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Let}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitLet(SimpleParser.LetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Try}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterTry(SimpleParser.TryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Try}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitTry(SimpleParser.TryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Switch}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void enterSwitch(SimpleParser.SwitchContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Switch}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 */
	void exitSwitch(SimpleParser.SwitchContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#assig}.
	 * @param ctx the parse tree
	 */
	void enterAssig(SimpleParser.AssigContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#assig}.
	 * @param ctx the parse tree
	 */
	void exitAssig(SimpleParser.AssigContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Symbol}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 */
	void enterSymbol(SimpleParser.SymbolContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Symbol}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 */
	void exitSymbol(SimpleParser.SymbolContext ctx);
	/**
	 * Enter a parse tree produced by the {@code String}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 */
	void enterString(SimpleParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code String}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 */
	void exitString(SimpleParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code Number}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 */
	void enterNumber(SimpleParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by the {@code Number}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 */
	void exitNumber(SimpleParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#handler}.
	 * @param ctx the parse tree
	 */
	void enterHandler(SimpleParser.HandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#handler}.
	 * @param ctx the parse tree
	 */
	void exitHandler(SimpleParser.HandlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#branch}.
	 * @param ctx the parse tree
	 */
	void enterBranch(SimpleParser.BranchContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#branch}.
	 * @param ctx the parse tree
	 */
	void exitBranch(SimpleParser.BranchContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#extracts}.
	 * @param ctx the parse tree
	 */
	void enterExtracts(SimpleParser.ExtractsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#extracts}.
	 * @param ctx the parse tree
	 */
	void exitExtracts(SimpleParser.ExtractsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#args}.
	 * @param ctx the parse tree
	 */
	void enterArgs(SimpleParser.ArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#args}.
	 * @param ctx the parse tree
	 */
	void exitArgs(SimpleParser.ArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#capability}.
	 * @param ctx the parse tree
	 */
	void enterCapability(SimpleParser.CapabilityContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#capability}.
	 * @param ctx the parse tree
	 */
	void exitCapability(SimpleParser.CapabilityContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#genericArgs}.
	 * @param ctx the parse tree
	 */
	void enterGenericArgs(SimpleParser.GenericArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#genericArgs}.
	 * @param ctx the parse tree
	 */
	void exitGenericArgs(SimpleParser.GenericArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#genericDef}.
	 * @param ctx the parse tree
	 */
	void enterGenericDef(SimpleParser.GenericDefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#genericDef}.
	 * @param ctx the parse tree
	 */
	void exitGenericDef(SimpleParser.GenericDefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#typeRef}.
	 * @param ctx the parse tree
	 */
	void enterTypeRef(SimpleParser.TypeRefContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#typeRef}.
	 * @param ctx the parse tree
	 */
	void exitTypeRef(SimpleParser.TypeRefContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#typeRefArgs}.
	 * @param ctx the parse tree
	 */
	void enterTypeRefArgs(SimpleParser.TypeRefArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#typeRefArgs}.
	 * @param ctx the parse tree
	 */
	void exitTypeRefArgs(SimpleParser.TypeRefArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link SimpleParser#path}.
	 * @param ctx the parse tree
	 */
	void enterPath(SimpleParser.PathContext ctx);
	/**
	 * Exit a parse tree produced by {@link SimpleParser#path}.
	 * @param ctx the parse tree
	 */
	void exitPath(SimpleParser.PathContext ctx);
}