package jay.aenigma.ckii;

import at.unisalzburg.dbresearch.apted.costmodel.*;
import at.unisalzburg.dbresearch.apted.distance.APTED;
import at.unisalzburg.dbresearch.apted.node.*;

import jay.aenigma.ckii.parser.*;

import org.antlr.v4.runtime.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class TreeNode extends Node<StringNodeData>{
	
	public static final String BLOCK = "{}";
	
	int annotation = -1;
	private int post_id;
	
	public TreeNode(String label){
		super(new StringNodeData(Objects.requireNonNull(label)));
	}
	
	private int identify(int start){
		for(int i = 0; i < getNumChildren(); i++){
			TreeNode treeNode = getChildNode(i);
			start = treeNode.identify(start);
		}
		post_id = start;
		start++;
		return start;
	}
	
	private int getNumChildren(){
		return getChildren().size();
	}
	
	private TreeNode getChildNode(int i){
		return (TreeNode) getChildren().get(i);
	}
	
	public boolean isRootNode(){
		return getNodeData().getLabel().isEmpty();
	}
	
	public boolean isTerminalNode(){
		return getNumChildren() == 0;
	}
	
	public boolean isBlockNode(){
		return BLOCK.equals(getNodeData().getLabel());
	}
	
	private TreeNode findByPostId(int post_id){
		if(this.post_id == post_id){
			return this;
		}
		else {
			for(int i = 0; i < getNumChildren(); i++){
				TreeNode node = getChildNode(i).findByPostId(post_id);
				if(node != null){
					return node;
				}
			}
			return null;
		}
	}
	
	@SuppressWarnings("ConstantConditions")
	public void computeEditMapping(TreeNode other){
		// apted
		APTED<PerEditOperationStringNodeDataCostModel, StringNodeData> apted
				= new APTED<>(new PerEditOperationStringNodeDataCostModel(1,1,3));
		apted.computeEditDistance(this, other);
		List<int[]> editMapping = apted.computeEditMapping();
		// annotate nodes
		this.identify(1);
		other.identify(1);
		int n = editMapping.size();
		for(int i = 0; i < n; i++){
			int[] ints = editMapping.get(i);
			if(ints[0] != 0 && ints[1] != 0){
				TreeNode left = this.findByPostId(ints[0]);
				left.annotation = n-i;
				TreeNode right = other.findByPostId(ints[1]);
				right.annotation = n-i;
			}
			else if(ints[0] != 0 && ints[1] == 0){
				TreeNode left = this.findByPostId(ints[0]);
				left.annotation = 0;
			}
			else if(ints[0] == 0 && ints[1] != 0){
				TreeNode right = other.findByPostId(ints[1]);
				right.annotation = 0;
			}
		}
	}
	
	public Deque<Integer> annotations(){
		if(isTerminalNode()){
			Deque<Integer> integers = new ArrayDeque<>();
			integers.add(annotation);
			return integers;
		}
		else if(isRootNode()){
			// Root node does not add any lines
			Deque<Integer> integers = new ArrayDeque<>();
			for(int i = 0; i < getNumChildren(); i++){
				integers.addAll(getChildNode(i).annotations());
			}
			return integers;
		}
		else {
			Deque<Integer> integers = new ArrayDeque<>();
			integers.add(annotation);
			for(int i = 0; i < getNumChildren(); i++){
				integers.addAll(getChildNode(i).annotations());
			}
			integers.add(annotation);
			return integers;
		}
	}
	
	public Deque<String> toStrings(){
		if(isTerminalNode()){
			Deque<String> strings = new ArrayDeque<>();
			strings.add(getNodeData().getLabel());
			return strings;
		} else if(isRootNode()){
			// Root node does not add any lines
			Deque<String> strings = new ArrayDeque<>();
			for(int i = 0; i < getNumChildren(); i++){
				strings.addAll(getChildNode(i).toStrings());
			}
			return strings;
		}else {
			Deque<String> strings = new ArrayDeque<>();
			strings.add(isBlockNode() ? "{" : getNodeData().getLabel() + "{");
			for(int i = 0; i < getNumChildren(); i++){
				Deque<String> stringDeque = getChildNode(i).toStrings();
				while(!stringDeque.isEmpty()){
					strings.add("\t" + stringDeque.removeFirst());
				}
			}
			strings.add("}");
			return strings;
		}
	}
	
	public static Predicate<String> makeLabelMatcher(String label){
		return Pattern.compile(String.format("^%s = $", label)).asPredicate();
	}
	
	public static Predicate<String> makeLabelNameMatcher(String label, String name){
		return Pattern.compile(String.format("^%s [=<>]=? %s$", label, name)).asPredicate();
	}
	
	public TreeNode findByLabel(Predicate<String> labelMatcher){
		if(labelMatcher.test(getNodeData().getLabel())){
			return this;
		}
		else {
			for(int i = 0; i < getNumChildren(); i++){
				TreeNode node = getChildNode(i).findByLabel(labelMatcher);
				if(node != null){
					return node;
				}
			}
			return null;
		}
	}
	
	public TreeNode findByNameField(Predicate<String> labelNameMatcher){
		if(!isTerminalNode()){
			for(int i = 0; i < getNumChildren(); i++){
				if(labelNameMatcher.test(getChildNode(i).getNodeData().getLabel())){
					return this;
				}
			}
			// else if the above fails
			for(int i = 0; i < getNumChildren(); i++){
				TreeNode node = getChildNode(i).findByNameField(labelNameMatcher);
				if(node != null){
					return node;
				}
			}
		}
		return null;
	}
	
	public boolean deepEquals(TreeNode other){
		if(!getNodeData().getLabel().equals(other.getNodeData().getLabel())){
			return false;
		}
		if(getNumChildren() != other.getNumChildren()){
			return false;
		}
		return IntStream.range(0, getNumChildren()).allMatch(value ->
				getChildNode(value).deepEquals(other.getChildNode(value))
		);
	}
	
	@Override
	public String toString(){
		return String.join("\r\n", this.toStrings());
	}
	
	public static TreeNode valueOf(String string){
		return valueOf(CharStreams.fromString(string));
	}
	
	public static TreeNode valueOf(CharStream charStream){
		CkiiLexer lexer = new CkiiLexer(charStream);
		CkiiParser parser = new CkiiParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new DefaultErrorStrategy());
		CkiiParser.UnitContext unit = parser.unit();
		
		NodeTreeVisitor visitor = new NodeTreeVisitor();
		return visitor.visit(unit);
	}
}
