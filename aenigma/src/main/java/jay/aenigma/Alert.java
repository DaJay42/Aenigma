package jay.aenigma;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Class whose instances represent problems in the User's setup, such as malformed files or conflicts.
 */
public class Alert{
	
	private Severity severity;
	private Kind kind;
	private List<Mod.ModFile> files;
	private GameFolder gameFolder;
	private String definitionName;
	
	private String message;
	
	private static final Comparator<Mod.ModFile> COMPARATOR_MOD_FILE =
			Comparator.comparing(Mod.ModFile::getName);
	private static final Comparator<Mod.ModFile> COMPARATOR_MOD_NAME =
			Comparator.comparing(modFile -> modFile.getMod().getName());
	
	/**
	 * Enum that identifies different causes of problems.
	 */
	public enum Kind {
		PARSE_ERROR("Parse error"),
		FILE_CONFLICT("File conflict"),
		NAME_CONFLICT("Name conflict"),
		UNDEFINE("Missing definition");
		
		String string;
		
		Kind(String string){
			this.string = string;
		}
		@Override
		public String toString(){
			return string;
		}
		
	}
	/**Creates a new Alert with the given parameters.<p/>
	 * The parameter files is copied, so future changes to it will not be reflected in this Alert.
	 * @param severity the {@link Severity} this Alert is classified as
	 * @param kind the {@link Kind} of cause this Alert has
	 * @param gameFolder the {@link GameFolder} this Alert has occurred in.
	 * @param files the {@link List} of {@link Mod.ModFile}s involved in this problem
	 * @param definitionName name of {@link Definition} involved in this, if any.
	 * @param message user-directed detail message
	 */
	public Alert(Severity severity, Kind kind, GameFolder gameFolder, List<Mod.ModFile> files, String definitionName, String message){
		this.severity = severity;
		this.kind = kind;
		this.gameFolder = gameFolder;
		this.definitionName = definitionName;
		List<Mod.ModFile> fileList = new ArrayList<>(files);
		fileList.sort(COMPARATOR_MOD_FILE.thenComparing(COMPARATOR_MOD_NAME));
		this.files = List.copyOf(fileList);
		this.message = message;
	}
	
	/**Retrieves the {@link Severity} of this Alert
	 * @return Severity of this Alert
	 */
	public Severity getSeverity(){
		return severity;
	}
	
	/**Retrieves the {@link Kind} of this Alert.
	 * @return Kind of this Alert
	 */
	public Kind getKind(){
		return kind;
	}
	
	/**Retrieves the {@link GameFolder} that this Alert occurred in.
	 * @return GameFolder that this occurred in
	 */
	public GameFolder getGameFolder(){
		return gameFolder;
	}
	
	/**Retrieves the {@link List} of {@link jay.aenigma.Mod.ModFile}s that are involved in this
	 * @return List of involved ModFiles
	 */
	public List<Mod.ModFile> getFiles(){
		return files;
	}
	
	/**Retrieves the Name of the {@link Definition} involved in this, if any; null otherwise.
	 * @return the Definition's Name or null
	 */
	public String getDefinitionName(){
		return definitionName;
	}
	
	/**Retrieves the user-directed detail message
	 * @return the message
	 */
	public String getMessage(){
		return message;
	}
	
	/**Generates a nicely-formatted {@link String} description of the {@link jay.aenigma.Mod.ModFile}s involved in this.
	 * @return String describing ModFiles
	 */
	public String formatFiles(){
		StringBuilder stringBuilder = new StringBuilder();
		for(Mod.ModFile file : files){
			stringBuilder.append(file.getName());
			stringBuilder.append("\" in \"");
			stringBuilder.append(file.getMod().getName());
			stringBuilder.append("\"");
			stringBuilder.append(System.lineSeparator());
		}
		return stringBuilder.toString();
	}
	
	@Override
	public String toString(){
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(severity.toString());
		stringBuilder.append(": ");
		stringBuilder.append(kind.toString());
		stringBuilder.append(" - ");
		stringBuilder.append(message);
		stringBuilder.append(System.lineSeparator());
		stringBuilder.append("The offending files are:");
		stringBuilder.append(System.lineSeparator());
		for(Mod.ModFile file : files){
			stringBuilder.append("\t\"");
			stringBuilder.append(file.getName());
			stringBuilder.append("\" in \"");
			stringBuilder.append(file.getMod().getName());
			stringBuilder.append("\"");
			stringBuilder.append(System.lineSeparator());
		}
		stringBuilder.append(System.lineSeparator());
		return stringBuilder.toString();
	}
}
