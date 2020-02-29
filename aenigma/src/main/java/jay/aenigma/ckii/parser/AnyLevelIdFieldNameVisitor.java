package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that gathers the names of the top-level elements of this.
 * Used for indexing traits, scripted_triggers/effects, ... etc
 */
public class AnyLevelIdFieldNameVisitor extends CkiiBaseVisitor<List<String>>{
	
	public String target = null;
	
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
}
