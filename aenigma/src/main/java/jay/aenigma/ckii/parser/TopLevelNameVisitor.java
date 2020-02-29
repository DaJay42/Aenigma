package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that gathers the names of the top-level elements of this.
 * Used for indexing traits, scripted_triggers/effects, ... etc
 */
public class TopLevelNameVisitor extends CkiiBaseVisitor<List<String>>{
	
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
		List<String> strings = defaultResult();
		strings.add(ctx.lhs.getText());
		return strings;
	}
}
