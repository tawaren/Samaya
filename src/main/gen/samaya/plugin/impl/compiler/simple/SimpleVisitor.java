// Generated from C:/Users/Markus Knecht/Dropbox/Privat/UZH/PhD/code/Samaya/src/main/antlr/samaya/plugin/impl/compiler/simple\Simple.g4 by ANTLR 4.7.2
package samaya.plugin.impl.compiler.simple;

    package samaya.plugin.impl.compiler.simple;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SimpleParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SimpleVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SimpleParser#file}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFile(SimpleParser.FileContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#import_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImport_(SimpleParser.Import_Context ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#wildcard}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWildcard(SimpleParser.WildcardContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#module}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModule(SimpleParser.ModuleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#component}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComponent(SimpleParser.ComponentContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Data}
	 * labeled alternative in {@link SimpleParser#dataDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData(SimpleParser.DataContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Function}
	 * labeled alternative in {@link SimpleParser#functionDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction(SimpleParser.FunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Error}
	 * labeled alternative in {@link SimpleParser#errorDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError(SimpleParser.ErrorContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#ext}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExt(SimpleParser.ExtContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#litSize}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLitSize(SimpleParser.LitSizeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#ctrs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtrs(SimpleParser.CtrsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#ctr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCtr(SimpleParser.CtrContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#fields}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFields(SimpleParser.FieldsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(SimpleParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Public}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPublic(SimpleParser.PublicContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Private}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrivate(SimpleParser.PrivateContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Protected}
	 * labeled alternative in {@link SimpleParser#visibility}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProtected(SimpleParser.ProtectedContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#risky}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRisky(SimpleParser.RiskyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#protection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProtection(SimpleParser.ProtectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#params}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParams(SimpleParser.ParamsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParam(SimpleParser.ParamContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#rets}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRets(SimpleParser.RetsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#ret}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRet(SimpleParser.RetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#borrows}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBorrows(SimpleParser.BorrowsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#funBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunBody(SimpleParser.FunBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#code}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCode(SimpleParser.CodeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#r_return}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitR_return(SimpleParser.R_returnContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#r_throw}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitR_throw(SimpleParser.R_throwContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Pack}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPack(SimpleParser.PackContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Literal}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(SimpleParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Invoke}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvoke(SimpleParser.InvokeContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Get}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGet(SimpleParser.GetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Fetch}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch(SimpleParser.FetchContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Unpack}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpack(SimpleParser.UnpackContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Image}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImage(SimpleParser.ImageContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExtractImage}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtractImage(SimpleParser.ExtractImageContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Let}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLet(SimpleParser.LetContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Try}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTry(SimpleParser.TryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Switch}
	 * labeled alternative in {@link SimpleParser#stm}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch(SimpleParser.SwitchContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#assig}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssig(SimpleParser.AssigContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Symbol}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbol(SimpleParser.SymbolContext ctx);
	/**
	 * Visit a parse tree produced by the {@code String}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(SimpleParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code Number}
	 * labeled alternative in {@link SimpleParser#lit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumber(SimpleParser.NumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#handler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHandler(SimpleParser.HandlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#branch}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBranch(SimpleParser.BranchContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#extracts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtracts(SimpleParser.ExtractsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#args}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgs(SimpleParser.ArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#capability}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCapability(SimpleParser.CapabilityContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#genericArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericArgs(SimpleParser.GenericArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#genericDef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenericDef(SimpleParser.GenericDefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#typeRef}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeRef(SimpleParser.TypeRefContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#typeRefArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeRefArgs(SimpleParser.TypeRefArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SimpleParser#path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPath(SimpleParser.PathContext ctx);
}