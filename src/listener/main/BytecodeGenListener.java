package listener.main;

import java.util.Hashtable;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.ProgramContext;
import generated.MiniCParser.StmtContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

import static listener.main.BytecodeGenListenerHelper.*;
import static listener.main.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();
	
	int tab = 0;
	int label = 0;
	
	// program	: decl+
	
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		symbolTable.initFunDecl();
		
		String fname = getFunName(ctx);
		ParamsContext params;
		
		if (fname.equals("main")) {
			symbolTable.putLocalVar("args", Type.INTARRAY);
		} else {
			symbolTable.putFunSpecStr(ctx);
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			symbolTable.putParams(params);
		}		
	}

	
	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		
		if (isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
			symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putGlobalVar(varName, Type.INT);
		}
	}

	
	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {			
		if (isArrayDecl(ctx)) {
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
		}
		else if (isDeclWithInit(ctx)) {
			symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));	
		}
		else  { // simple decl
			symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
		}	
	}

	
	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		String classProlog = getFunProlog();
		
		String fun_decl = "", var_decl = "";
		
		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(isFunDecl(ctx, i))
				fun_decl += newTexts.get(ctx.decl(i));
			else
				var_decl += newTexts.get(ctx.decl(i));
		}
		
		newTexts.put(ctx, classProlog + var_decl + fun_decl);
		
		System.out.println(newTexts.get(ctx));
	}	
	
	
	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)				//var_decl
				decl += newTexts.get(ctx.var_decl());
			else							//fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}
	
	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			// <(0) Fill here>	
			else if(ctx.if_stmt() != null)
				stmt += newTexts.get(ctx.if_stmt());  // if_stmt이면 ctx의 if_stmt()를 newTexts에서 가져와 stmt에 저장합니다.
			else if(ctx.while_stmt() != null)
				stmt += newTexts.get(ctx.while_stmt());  // while_stmt이면 ctx의 while_stmt()를 newTexts에서 가져와 stmt에 저장합니다.
			else 
				stmt += newTexts.get(ctx.return_stmt());  // return_stmt이면 ctx의 return_stmt()를 newTexts에서 가져와 stmt에 저장합니다.
		}
		newTexts.put(ctx, stmt); 
	}
	
	// expr_stmt	: expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}
	
	
	// while_stmt	: WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
			// <(1) Fill here!> 
		String Loop = symbolTable.newLabel();  // 반복되는 loop를 의미할 label로 Loop를 지정합니다.
		String End = symbolTable.newLabel();  // loop를 빠져나오는 것을 의미할 label을 End로 지정합니다.
		String expr = newTexts.get(ctx.expr()); // ctx의 expr을 newTexts에서 가져와 expr에 저장합니다.
		String stmt = newTexts.get(ctx.stmt());  // ctx의 stmt을 newTexts에서 가져와 stmt에 저장합니다.
		String str = ""; 
		// Loop로 반복되는 부분을 시작, ifeq로 반복문 조건에 맞는지 확인, 맞지 않으면 End로 가서 반복문 종료합니다.
		// 맞으면 stmt 출력, 다시 Loop로 가서 반복합니다.
		str += Loop+":\n" + expr + "ifeq "+End+"\n" + stmt + "\ngoto "+ Loop + "\n" + End + ":\n";
		newTexts.put(ctx, str); 
	}
	
	
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
			// <(2) Fill here!>
		String IDENT = ctx.IDENT().getText(); //ctx의 IDENT(함수이름)를 가져와 IDENT에 저장합니다.
		String compound = newTexts.get(ctx.compound_stmt());  // ctx의 compound_stmt를 newTexts에서 가져와 compound에 저장합니다.
		String str = "";
		str += "\n"+funcHeader(ctx,IDENT)+compound+".end method\n"; // 함수의 헤더와 함수 안의 내용(compound), 함수의 끝을 의미합니다.
		newTexts.put(ctx, str);
	}
	

	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"	
				+ "\t" + ".limit stack " 	+ getStackSize(ctx) + "\n"
				+ "\t" + ".limit locals " 	+ getLocalVarSize(ctx) + "\n";
				 	
	}
	
	
	
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}
	
	
	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		
		if (isDeclWithInit(ctx)) {
			String vId = symbolTable.getVarId(ctx);
			varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
					+ "istore_" + vId + "\n"; 			
		}
		
		newTexts.put(ctx, varDecl);
	}

	
	// compound_stmt	: '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
		// <(3) Fill here>
		String localDecl = "";
		String stmt = "";
		for(int i =0; i < ctx.local_decl().size(); i++) { 
			localDecl += newTexts.get(ctx.local_decl(i))+"\n";  // local_decl의 수만큼 local_decl을 newTexts에서 가져와 localDecl에 추가합니다.
		}
		for(int i =0; i < ctx.stmt().size(); i++) { 
			stmt += newTexts.get(ctx.stmt(i))+"\n"; // stmt의 수만큼 stmt를 newTexts에서 가져와 stmt에 저장합니다.
		}
		newTexts.put(ctx, "\n"+localDecl+stmt); // localDecl과 stmt를 newTexts에 put합니다.
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		String condExpr= newTexts.get(ctx.expr());
		String thenStmt = newTexts.get(ctx.stmt(0));
		
		String lend = symbolTable.newLabel();
		String lelse = symbolTable.newLabel();
		
		
		if(noElse(ctx)) {		
			
			stmt += condExpr + "\n"
				+ "ifeq " + lend + "\n"
				+ thenStmt + "\n"
				+ lend + ":"  + "\n";	
		}
		else {
			
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "ifeq " + lelse + "\n"
					+ thenStmt + "\n"
					+ "goto " + lend + "\n"
					+ lelse + ": " + "\n"
					+ elseStmt + "\n"
					+ lend + ":"  + "\n";	
		}
		
		newTexts.put(ctx, stmt);
	}
	
	
	// return_stmt	: RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
			// <(4) Fill here>
		String str = "";
		// child 개수가 2개이면 (RETRUN ';')
		if(ctx.getChildCount() == 2) {
			str += "return"; // return을 str에 저장	 
		}
		// RETURN expr ';'이면
		else {
			String expr = newTexts.get(ctx.expr()); // expr을 newTexts에서 가져와 expr에 저장
			str += expr + "\n" + "ireturn";  // expr과 ireturn을 str에 저장
		}
		newTexts.put(ctx, str);
	}

	
	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";

		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}		
		
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			if(ctx.IDENT() != null) {
				String idName = ctx.IDENT().getText();
				if(symbolTable.getVarType(idName) == Type.INT) {
					expr += "iload_" + symbolTable.getVarId(idName) + "\n";
				}
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
			} else if (ctx.LITERAL() != null) {
				String literalStr = ctx.LITERAL().getText();
				expr += "ldc " + literalStr + " \n";
			}
		} else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, expr);	
		} else if(ctx.getChildCount() == 3) {	 
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));
				
			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
				
			} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
				
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else { // expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
		}
		newTexts.put(ctx, expr);
	}


	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String l1 = symbolTable.newLabel();
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		String ar[];
		String store = "";
		
		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText()) {
		case "-":
			expr += "ineg \n"; 
			break;
		case "--":
			ar = expr.trim().split("_");
			store += ar[1];
			expr += "ldc 1" + "\n"
					+ "isub" + "\n"
					+ "istore_" + store + "\n";
			break;
		case "++":
			ar = expr.trim().split("_");
			store += ar[1];
			expr += "ldc 1" + "\n"
					+ "iadd" + "\n"
					+ "istore_" + store + "\n";
			break;
		case "!":
			expr += "ifeq " + l2 + "\n"
					+ l1 + ": " + "\n"
					+ "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": " + "\n"
					+ "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		String end = "";
		
		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));
		
		switch (ctx.getChild(1).getText()) {
			case "*":
				expr += "imul \n"; break;
			case "/":
				expr += "idiv \n"; break;
			case "%":
				expr += "irem \n"; break;
			case "+":		// expr(0) expr(1) iadd
				expr += "iadd \n"; break;
			case "-":
				expr += "isub \n"; break;
				
			case "==":
				expr += "isub " + "\n"
						+ "ifeq " +l2 + "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n"
						+ "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;
			case "!=":
				expr += "isub " + "\n"
						+ "ifne "+l2+ "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n" 
						+ "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;
			case "<=":
				// <(5) Fill here>
				// 두개의 expr을 빼서(앞-뒤) 그것이 0보다 작거나 같으면 성립하므로 l2로 가서 1을 불러옵니다.
				// 작거나 같지 않으면 성립하지 않으므로 0을 불러오고 lend로 갑니다.
				expr += "isub " + "\n"
						+ "ifle "+l2+ "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n" 
						+ "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;
			case "<":
				// <(6) Fill here>
				// 두개의 expr을 빼서(앞-뒤) 그것이 0보다 작으면 성립하므로 l2로 가서 1을 불러옵니다.
				// 작지 않으면 성립하지 않으므로 0을 불러오고 lend로 갑니다.
				expr += "isub " + "\n"
						+ "iflt "+l2+ "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n"
						+ "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;

			case ">=":
				// <(7) Fill here>
				// 두개의 expr을 빼서(앞-뒤) 그것의 0보다 크거나 같으면 성립하므로 l2로 가서 1을 불러옵니다.
				// 크거나 같지 않으면 성립하지 않으므로 0을 불러오고 lend로 갑니다.
				expr += "isub " + "\n"
						+ "ifge "+l2+ "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n"
						+ "ldc 1" + "\n"
						+ lend + ": " + "\n";

				break;

			case ">":
				// <(8) Fill here>
				// 두개의 expr을 빼서(앞-뒤) 그것이 0보다 크면 성립하므로 l2로 가서 1을 불러옵니다.
				// 크지 않으면 성립하지 않으므로 0을 불러오고 lend로 갑니다.
				expr += "isub " + "\n"
						+ "ifgt "+l2+ "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "\n"
						+ "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;

			case "and":
				expr = "";
				expr += newTexts.get(ctx.expr(0));
				end += symbolTable.newLabel();
				expr += "ifeq " + end + "\n"
						+ "goto " + l2 + "\n"
						+ l2 + ": " + "\n"
						+ newTexts.get(ctx.expr(1)) + "\n"
						+ "ifeq " + end + "\n"
						+ "ldc 1" + "\n"
						+ "goto " + lend + "\n"
						+ end + ":" + "\n"
						+ "ldc 0\n"
						+ lend + ": " + "\n";
				break;
			case "or":
				// <(9) Fill here>
				expr = "";
				expr += newTexts.get(ctx.expr(0)); // 첫번째 expr을 newText에서 가져와 expr에 저장합니다.
				end += symbolTable.newLabel(); // 새로운 label을 가져와 end에 저장합니다.
				// 첫번째 expr이 0이면 두번째 expr을 확인해야하므로 l2로 갑니다.
				// l2에서 두번째 expr을 newTexts에서 가져옵니다. 
				// 그것이 0과 같지 않으면 1이므로 or 조건에 맞아서 end로 이동해 1을 불러옵니다.
				// 첫번째 expr이 0이 아니라 1인 경우에는 or 조건에 맞아서 바로 end로 이동해 1을 불러옵니다.
				expr += "ifeq " + l2 + "\n"
						+ "goto " + end + "\n"
						+ l2 + ": " + "\n"
						+ newTexts.get(ctx.expr(1)) + "\n"
						+ "ifne " + end + "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ end + ":" + "\n"
						+ "ldc 1\n"
						+ lend + ": " + "\n";
				break;

		}
		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		String fname = getFunName(ctx);		

		if (fname.equals("_print")) {		// System.out.println	
			expr = "getstatic java/lang/System/out Ljava/io/PrintStream; " + "\n"
			  		+ newTexts.get(ctx.args()) 
			  		+ "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
		} else {	
			expr = newTexts.get(ctx.args()) 
					+ "invokestatic " + getCurrentClassName()+ "/" + symbolTable.getFunSpecStr(fname) + "\n";
		}	
		
		return expr;
			
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {

		String argsStr = "\n";
		
		for (int i=0; i < ctx.expr().size() ; i++) {
			argsStr += newTexts.get(ctx.expr(i)) ; 
		}		
		newTexts.put(ctx, argsStr);
	}

}
