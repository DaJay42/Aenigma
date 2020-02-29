package jay.aenigma;

/**
 * Class representing TODO: words
 */
public class Definition{
	private String name;
	private Mod.ModFile file;
	private MergeBehaviour mergeBehaviour;
	
	/**Create a new Definition with the given parameters
	 * @param name Name of the Definition, as given in the ModFile
	 * @param file ModFile that the Definition originates from
	 * @param mergeBehaviour mergeBehaviour that the Definition exhibits
	 */
	public Definition(String name, Mod.ModFile file, MergeBehaviour mergeBehaviour){
		this.name = name;
		this.file = file;
		this.mergeBehaviour = mergeBehaviour;
	}
	
	/**Retrieves the {@link String} representing this Definitions Name, as found in its {@link jay.aenigma.Mod.ModFile}
	 * @return the name
	 */
	public String getName(){
		return name;
	}
	
	/**Retrieves the {@link jay.aenigma.Mod.ModFile} that this Definition resides in.
	 * @return the ModFile
	 */
	public Mod.ModFile getFile(){
		return file;
	}
	
	/**Retrieves the {@link GameFolder} that this Definition belongs to
	 * @return the GameFolder
	 */
	public GameFolder getGameFolder(){
		return file.getGameFolder();
	}
	
	/**Retrieves the {@link Mod} that this Definition originates from.
	 * @return the Mod
	 */
	public Mod getMod(){
		return file.getMod();
	}
	
	/**Retrieves the {@link MergeBehaviour} that this Definition exhibits.
	 * @return the MergeBehaviour
	 */
	public MergeBehaviour getMergeBehaviour(){
		return mergeBehaviour;
	}
	
	@Override
	public String toString(){
		return name;
	}
	
	public boolean isEquivalent(Definition other){
		return file.gameFolder.equals(other.file.gameFolder) && name.equals(other.name);
	}
}
