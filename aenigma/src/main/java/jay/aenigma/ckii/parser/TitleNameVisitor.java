package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Visitor that gathers the names of Titles
 */
public class TitleNameVisitor extends CkiiBaseVisitor<List<String>>{
	
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
		List<String> strings = super.visitBlockStatement(ctx);
		String name = ctx.lhs.getText();
		if(name.startsWith("b_") || name.startsWith("c_") || name.startsWith("d_") || name.startsWith("k_") || name.startsWith("e_"))
			strings.add(name);
		return strings;
	}
}
