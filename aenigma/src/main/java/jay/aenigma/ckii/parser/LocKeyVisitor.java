package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

public class LocKeyVisitor extends CkiiLocBaseVisitor<List<String>>{
	
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
	public List<String> visitLocalisation(CkiiLocParser.LocalisationContext ctx){
		List<String> strings = defaultResult();
		strings.add(ctx.KEY().getText());
		return strings;
	}
}
