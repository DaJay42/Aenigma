package jay.aenigma;

/**
 * Enum describing the behaviour of the {@link Game} when encountering {@link Definition}-level conflicts
 * in a given {@link GameFolder}, i.e. Definitions of the same Name but in distinctly named {@link jay.aenigma.Mod.ModFile}s.
 */
public enum MergeBehaviour{
	NOT_APPLICABLE			("These are file-local; conflict resolution doesn't apply.", Severity.TRIVIAL),
	MERGE_GROUPS			("These groups will be merged.", Severity.INFO),
	PARTIAL_COMPLEX			("This will cause complex merging behaviour.", Severity.INFO),
	REPLACE					("The last one replaces all other.", Severity.INFO),
	IGNORED					("All but the first will be ignored.", Severity.INFO),
	@Deprecated //Ideally, the UNKNOWN type should never need to be used. In reality however...
	UNKNOWN					("This is not known to be safe\n" +
										" and may have unexpected effects.", Severity.WARNING),
	UNSAFE					("This is considered unsafe\n" +
										" and may unexpectedly fail.", Severity.WARNING),
	PARTIAL_COMPLEX_MAYBE	("This will cause complex merging behaviour\n" +
										" that may or may not be intended.", Severity.WARNING),
	FAIL_ON_DUPLICATE		("This is an error and will cause unpredictable results.", Severity.ERROR),
	NONE					("This is not a valid operation.", Severity.ERROR),
	;
	String description;
	Severity severity;
	
	MergeBehaviour(String description, Severity severity){
		this.description = description;
		this.severity = severity;
	}
}
