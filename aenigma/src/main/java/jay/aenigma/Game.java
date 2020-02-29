package jay.aenigma;

import jay.aenigma.ckii.CkiiFolder;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Enum representing the Games that this program can work with.
 */
public enum Game{
	CK2("Crusader Kings II", 203770, "CK2game.exe", CkiiFolder::values),
	//Stellaris("Stellaris", 281990, "stellaris.exe", GameFolder[]::new)) // TODO: once that fustercluck of a launcher stabilizes into something usable
	;
	
	private static final String PARADOX_INTERACTIVE = "Paradox Interactive";
	private static final String SETTINGS_TXT = "settings.txt";
	private static final String STEAM_APPS_APPMANIFEST_D_ACF = "SteamApps/appmanifest_%d.acf";
	
	private String folder;
	private int steamId;
	private String executable;
	private Supplier<GameFolder[]> gameFolders;
	
	Game(String folder, int steamId, String executable, Supplier<GameFolder[]> gameFolders){
		this.folder = folder;
		this.steamId = steamId;
		this.executable = executable;
		this.gameFolders = gameFolders;
	}
	
	/**Retrieves the {@link String} representation of the {@link java.io.File} name of this Game's executable
	 * @return name of Game's executable
	 */
	public String getExecutable(){
		return executable;
	}
	
	/**Retrieves the {@link Path} to this Game's User Data folder, assuming the given Path refers to the "My Documents" folder,
	 * or equivalent.
	 * <br/> Returns null iff docsFolder is null.
	 * @param docsFolder "My Documents" folder, or equivalent
	 * @return this Game's User Data folder
	 */
	public Path getUserDataFolder(Path docsFolder){
		return docsFolder != null ? docsFolder.resolve(PARADOX_INTERACTIVE).resolve(this.folder): null;
	}
	
	/**Retrieves the {@link Path} to this Game's Settings {@link java.io.File}, assuming the given Path refers to the "My Documents" folder,
	 * or equivalent.
	 * <br/> Returns null iff docsFolder is null.
	 * @param docsFolder "My Documents" folder, or equivalent
	 * @return this Game's Settings File
	 */
	public Path getSettingsFile(Path docsFolder){
		return docsFolder != null ? getUserDataFolder(docsFolder).resolve(SETTINGS_TXT) : null;
	}
	
	/**Retrieves the {@link Path} of this Game's Manifest File, given the Path to Steam's root folder.
	 * <br/> Returns null iff steamRoot is null.
	 * @param steamRoot Steam's root folder
	 * @return this Game's Manifest File
	 */
	public Path getManifestFile(Path steamRoot){
		return steamRoot != null ? steamRoot.resolve(String.format(STEAM_APPS_APPMANIFEST_D_ACF, this.steamId)) : null;
	}
	
	/**Retrieves the {@link Path} to this Game's Data Folder, i.e. where it's executable resides.
	 * <br/> Returns null iff installRoot is null.
	 * @param installRoot the parent of the folder where this Game's executable resides.
	 * @return the folder where this Game's executable resides.
	 */
	public Path getGameDataFolder(Path installRoot){
		return installRoot != null ? installRoot.resolve(this.folder) : null;
	}
	
	/**Retrieves the {@link GameFolder}s representing the possible categories of {@link jay.aenigma.Mod.ModFile}s
	 * for this Game.
	 * @return Array of GameFolders
	 */
	public GameFolder[] getGameFolders(){
		return gameFolders.get();
	}
	
	/**Returns the Name of this Game
	 * @return the Name of the Game
	 */
	@Override
	public String toString(){
		return this.folder;
	}
}
