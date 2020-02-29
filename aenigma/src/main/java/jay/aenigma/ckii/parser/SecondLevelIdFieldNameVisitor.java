package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that gathers the names of the second-level elements of this.
 * Used for indexing portraits, ... etc
 */
public class SecondLevelIdFieldNameVisitor extends CkiiBaseVisitor<List<String>>{
	
	public String target = null;
	
	private int level = 0;
	
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
	public List<String> visitExpressionStatement(CkiiParser.ExpressionStatementContext ctx){
		List<String> strings = defaultResult();
		
		if(target.equalsIgnoreCase(ctx.lhs.getText()))
			strings.add(ctx.rhs.getText());
		
		return strings;
	}
	
	@Override
	public List<String> visitBlockStatement(CkiiParser.BlockStatementContext ctx){
		List<String> strings;
		if(level < 2){
			level++;
			strings = super.visitBlockStatement(ctx);
			level--;
		}
		else {
			strings = defaultResult();
		}
		return strings;
	}
}
