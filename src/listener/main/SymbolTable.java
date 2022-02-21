package listener.main;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable.Type;
import static listener.main.BytecodeGenListenerHelper.*;


public class SymbolTable {
	enum Type {
		INT, INTARRAY, VOID, ERROR
	}
	
	static public class VarInfo {
		Type type; 
		int id;
		int initVal;
		
		public VarInfo(Type type,  int id, int initVal) {
			this.type = type;
			this.id = id;
			this.initVal = initVal;
		}
		public VarInfo(Type type,  int id) {
			this.type = type;
			this.id = id;
			this.initVal = 0;
		}
	}
	
	static public class FInfo {
		public String sigStr;
	}
	
	private Map<String, VarInfo> _lsymtable = new HashMap<>();	// local v.
	private Map<String, VarInfo> _gsymtable = new HashMap<>();	// global v.
	private Map<String, FInfo> _fsymtable = new HashMap<>();	// function 
	
		
	private int _globalVarID = 0;
	private int _localVarID = 0;
	private int _labelID = 0;
	private int _tempVarID = 0;
	
	SymbolTable(){
		initFunDecl();
		initFunTable();
	}
	
	void initFunDecl(){		// at each func decl
		_localVarID = 0;
		_labelID = 0;
		_tempVarID = 32;		
	}
	
	
	void putLocalVar(String varname, Type type){
		//<Fill here>
		// _lsymtable에 새로운 local 변수를 저장합니다.
		_lsymtable.put(varname, new VarInfo(type, _localVarID));
		// 새로운 변수를 추가했으므로 _localVarID에 1을 더합니다.
		_localVarID++;
	}
	
	void putGlobalVar(String varname, Type type){
		//<Fill here>
		// _gsymtable에 새로운 global 변수를 저장합니다.
		_gsymtable.put(varname, new VarInfo(type, _globalVarID));
		// 새로운 변수를 추가했으므로 _globalVarID에 1을 더합니다.
		_globalVarID++;
	}
	
	void putLocalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		// _lsymtable에 새로운 local 변수를 초기화값과 함께 저장합니다.
		_lsymtable.put(varname, new VarInfo(type, _localVarID, initVar));
		// 새로운 변수를 추가했으므로 _localVarID에 1을 더합니다.
		_localVarID++;
	}
	void putGlobalVarWithInitVal(String varname, Type type, int initVar){
		//<Fill here>
		// _gsymtable에 새로운 global 변수를 초기화값과 함께 저장합니다.
		_gsymtable.put(varname, new VarInfo(type, _globalVarID, initVar));
		// 새로운 변수를 추가했으므로 _globalVarID에 1을 더합니다.
		_globalVarID++;
	}
	
	void putParams(MiniCParser.ParamsContext params) {
		// params에 param이 하나라도 있으면
		if(params.param().size() > 0) {
			for(int i = 0; i < params.param().size(); i++) {
				//<Fill here>
				// param의 개수만큼 _lsymtable에 새로운 local 변수를 저장합니다.
				_lsymtable.put(params.param(i).IDENT().getText(), new VarInfo(Type.INT, _localVarID));
				// 새로운 변수를 추가했으므로 _localVarID에 1을 더합니다.
				_localVarID++;
			}
		}
		// params가 void이면
		else if(params.VOID() != null) {
			// _lsymtable에 void 형태로 새로운 local 변수를 저장합니다.
			_lsymtable.put(params.VOID().getText(), new VarInfo(Type.VOID, _localVarID));
			// 새로운 변수를 추가했으므로 _localVarID에 1을 더합니다.
			_localVarID++;
		}
	}
	
	private void initFunTable() {
		FInfo printlninfo = new FInfo();
		printlninfo.sigStr = "java/io/PrintStream/println(I)V";
		
		FInfo maininfo = new FInfo();
		maininfo.sigStr = "main([Ljava/lang/String;)V";
		_fsymtable.put("_print", printlninfo);
		_fsymtable.put("main", maininfo);
	}
	
	public String getFunSpecStr(String fname) {		
		// <Fill here>
		// _fsymtable에서 이름이 fname인 함수의 정보를 가져와 반환합니다.
		return _fsymtable.get(fname).sigStr;
	}

	public String getFunSpecStr(Fun_declContext ctx) {
		// <Fill here>	
		// ctx의 IDENT를 getText로 가져와 fname에 저장합니다.
		String fname = ctx.IDENT().getText();
		// _fsymtable에서 이름이 fname인 함수의 정보를 가져와 반환합니다.
		return _fsymtable.get(fname).sigStr;
	}
	
	public String putFunSpecStr(Fun_declContext ctx) {
		String fname = getFunName(ctx);
		String argtype = "";	
		String rtype = "";
		String res = "";
		
		// <Fill here>	
		String array[];
		// ctx의 params를 getText로 가져와 빈칸을 모두 없앱니다. 그리고 ,를 기준으로 잘라서 배열에 저장합니다.
		array = ctx.params().getText().trim().split(",");
		// array의 길이만큼 반복합니다.
		for(int i=0; i<array.length; i++) {
			// array[i]가 ""와 같으면(함수의 파라메터가 없으면)
			if(array[i].equals("")) {
				argtype += array[i]; // argtype에 array[i]을 저장합니다.
			}
			// 함수의 파라메터가 있으면
			else {
				// 파라메터의 첫번째 알파벳을 대문자로 바꿔서 argtype에 저장합니다.
				argtype += array[i].substring(0,1).toUpperCase();
			}
		}
		// ctx의 type_spec을 getText로 가져와 빈칸을 없애고 첫번째 알파벳을 대문자로 바꿔서 rtype에 저장합니다.
		rtype += ctx.type_spec().getText().trim().substring(0,1).toUpperCase();
		
		
		res =  fname + "(" + argtype + ")" + rtype;
		
		FInfo finfo = new FInfo();
		finfo.sigStr = res;
		_fsymtable.put(fname, finfo);
		
		return res;
	}
	
	String getVarId(String name){
		// <Fill here>	
		String id = "";
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		// name 이름의 변수가 local 변수이면
		if (lvar != null) {
			// 그 local 변수의 id를 문자열로 바꿔서 반환합니다.
			return Integer.toString(lvar.id);
		}
		
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		// name 이름의 변수가 global 변수이면
		if (gvar != null) {
			// 그 global 변수의 id를 문자열로 바꿔서 반환합니다.
			return Integer.toString(gvar.id);
		}
		
		return id;	
	}
	
	Type getVarType(String name){
		VarInfo lvar = (VarInfo) _lsymtable.get(name);
		if (lvar != null) {
			return lvar.type;
		}
		
		VarInfo gvar = (VarInfo) _gsymtable.get(name);
		if (gvar != null) {
			return gvar.type;
		}
		
		return Type.ERROR;	
	}
	String newLabel() {
		return "label" + _labelID++;
	}
	
	String newTempVar() {
		String id = "";
		return id + _tempVarID--;
	}

	// global
	public String getVarId(Var_declContext ctx) {
		// <Fill here>	
		String sname = "";
		// ctx의 IDENT를 getText로 가져와서 getVarId로 id를 가져와 sname에 저장합니다.
		sname += getVarId(ctx.IDENT().getText());
		// sname을 반환합니다.
		return sname;
	}

	// local
	public String getVarId(Local_declContext ctx) {
		String sname = "";
		sname += getVarId(ctx.IDENT().getText());
		return sname;
	}
}
