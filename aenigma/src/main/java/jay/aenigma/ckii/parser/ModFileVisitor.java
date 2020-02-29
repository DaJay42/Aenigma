package jay.aenigma.ckii.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Extract the mod list form the settings file
 */
public class ModFileVisitor extends CkiiBaseVisitor<List<String>>{
	
	
	private String name;
	private String path;
	private boolean zipped;
	
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
		if("dependencies".equals(ctx.lhs.getText()))
			return super.visitBlockStatement(ctx);
		else
			return defaultResult();
	}
	
	@Override
	public List<String> visitExpressionStatement(CkiiParser.ExpressionStatementContext ctx){
		List<String> strings = super.visitExpressionStatement(ctx);
		switch(ctx.lhs.getText()){
			case "name":
				name = strings.get(1);
				break;
			case "archive":
				path = strings.get(1);
				zipped = true;
				break;
			case "path":
				path = strings.get(1);
				zipped = false;
				break;
			default:
				break;
		}
		
		return defaultResult();
	}
	
	@Override
	public List<String> visitStringExpr(CkiiParser.StringExprContext ctx){
		List<String> strings = defaultResult();
		String s = ctx.getText();
		strings.add(s.substring(1, s.length()-1));
		return strings;
	}
	
	@Override
	public List<String> visitIdenExpr(CkiiParser.IdenExprContext ctx){
		List<String> strings = defaultResult();
		strings.add(ctx.getText());
		return strings;
	}
	
	public String getName(){
		return name;
	}
	
	public String getPath(){
		return path;
	}
	
	public boolean isZipped(){
		return zipped;
	}
}
