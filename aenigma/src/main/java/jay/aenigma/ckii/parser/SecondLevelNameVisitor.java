package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that gathers the names of the second-level elements of this.
 * Used for indexing religions, cultures, decisions, ... etc
 */
public class SecondLevelNameVisitor extends CkiiBaseVisitor<List<String>>{
	
	private boolean top = true;
	
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
		List<String> strings;
		if(top){
			top = false;
			strings = super.visitBlockStatement(ctx);
			top = true;
		}
		else{
			strings = defaultResult();
			strings.add(ctx.lhs.getText());
		}
		return strings;
	}
}
