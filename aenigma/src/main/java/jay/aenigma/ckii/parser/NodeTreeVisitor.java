package jay.aenigma.ckii.parser;

import at.unisalzburg.dbresearch.apted.node.Node;
import at.unisalzburg.dbresearch.apted.node.StringNodeData;
import jay.aenigma.ckii.TreeNode;

public class NodeTreeVisitor extends CkiiBaseVisitor<TreeNode>{
	
	@Override
	public TreeNode visitUnit(CkiiParser.UnitContext ctx){
		TreeNode node = new TreeNode("");
		for(CkiiParser.StatementContext statementContext : ctx.statement()){
			TreeNode child = statementContext.accept(this);
			if(child != null)
				node.addChild(child);
		}
		return node;
	}
	
	@Override
	public TreeNode visitBlockStatement(CkiiParser.BlockStatementContext ctx){
		TreeNode node;
		TreeNode accept = ctx.rhs.accept(this);
		if(accept.getChildren().size() > 0){
			node = new TreeNode(String.format("%s %s ", ctx.lhs.getText(), ctx.OPERATOR().getText()));
			for(Node<StringNodeData> child : accept.getChildren()){
				node.addChild(child);
			}
		}
		else { // accept is Empty block
			node = new TreeNode(String.format("%s %s %s", ctx.lhs.getText(), ctx.OPERATOR().getText(), accept.toString()));
		}
		return node;
	}
	
	@Override
	public TreeNode visitExpressionStatement(CkiiParser.ExpressionStatementContext ctx){
		return new TreeNode(ctx.lhs.getText() + " " + ctx.OPERATOR().getText() + " "+ ctx.rhs.getText());
	}
	
	@Override
	public TreeNode visitEmptyBlock(CkiiParser.EmptyBlockContext ctx){
		return new TreeNode(TreeNode.BLOCK);
	}
	
	@Override
	public TreeNode visitStatementBlock(CkiiParser.StatementBlockContext ctx){
		TreeNode node = new TreeNode(TreeNode.BLOCK);
		for(CkiiParser.StatementContext statementContext : ctx.statement()){
			TreeNode child = statementContext.accept(this);
			if(child != null)
				node.addChild(child);
		}
		return node;
	}
	
	@Override
	public TreeNode visitExpressionBlock(CkiiParser.ExpressionBlockContext ctx){
		TreeNode node = new TreeNode(TreeNode.BLOCK);
		for(CkiiParser.ExpressionContext expressionContext : ctx.expression()){
			TreeNode child = expressionContext.accept(this);
			if(child != null)
				node.addChild(child);
		}
		return node;
	}
	
	@Override
	public TreeNode visitBlockBlock(CkiiParser.BlockBlockContext ctx){
		TreeNode node = new TreeNode(TreeNode.BLOCK);
		for(CkiiParser.BlockContext blockContext : ctx.block()){
			TreeNode child = blockContext.accept(this);
			if(child != null)
				node.addChild(child);
		}
		return node;
	}
	
	@Override
	public TreeNode visitStringExpr(CkiiParser.StringExprContext ctx){
		return new TreeNode(ctx.getText());
	}
	
	@Override
	public TreeNode visitIdenExpr(CkiiParser.IdenExprContext ctx){
		return new TreeNode(ctx.getText());
	}
	
	@Override
	public TreeNode visitNamespExpr(CkiiParser.NamespExprContext ctx){
		return new TreeNode(ctx.getText());
	}
	
	@Override
	public TreeNode visitNumberExpr(CkiiParser.NumberExprContext ctx){
		return new TreeNode(ctx.getText());
	}
	
	@Override
	public TreeNode visitDateExpr(CkiiParser.DateExprContext ctx){
		return new TreeNode(ctx.getText());
	}
	
	@Override
	public TreeNode visitBoolExpr(CkiiParser.BoolExprContext ctx){
		return new TreeNode(ctx.getText());
	}
}
