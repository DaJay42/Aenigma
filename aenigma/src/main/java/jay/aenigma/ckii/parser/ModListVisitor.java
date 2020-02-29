package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract the mod list form the settings file
 */
public class ModListVisitor extends CkiiBaseVisitor<List<String>>{
	
	@Override
	protected List<String> defaultResult(){
		return new ArrayList<>();
	}
	
	@Override
	protected List<String> aggregateResult(List<String> aggregate, List<String> nextResult){
		aggregate.addAll(nextResult);
		return aggregate;
	}
	
	@Override
	public List<String> visitBlockStatement(CkiiParser.BlockStatementContext ctx){
		if("last_mods".equals(ctx.lhs.getText()))
			return super.visitBlockStatement(ctx);
		else
			return defaultResult();
	}
	
	@Override
	public List<String> visitExpressionStatement(CkiiParser.ExpressionStatementContext ctx){
		return defaultResult();
	}
	
	@Override
	public List<String> visitStringExpr(CkiiParser.StringExprContext ctx){
		List<String> strings = defaultResult();
		strings.add(ctx.getText());
		return strings;
	}
}
