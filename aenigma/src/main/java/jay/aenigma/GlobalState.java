package jay.aenigma;

import jay.aenigma.gui.Gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Non-instantiable class holding any application state that has yet to find a more appropriate place to call home.
 */
public final class GlobalState{
	
	/**Name of the Steam executable. Used for verifying {@link GlobalState#steamRoot}*/
	public static final String STEAM_EXE = "steam.exe";
	
	/** {@link ExecutorService} used for parallel processing of background tasks, such as parsing files,
	 * as well any single tasks that require too much time to be processed in the UI thread.*/
	public static ExecutorService backgroundExecutor = Executors.newWorkStealingPool();
	/** javafx-based graphical user interface*/
	public static Gui gui = null;
	
	/** set to true to log debug output*/
	public static boolean isDebug = true;
	/** minimal {@link Severity} of an {@link Alert} for it to be relayed to the user*/
	public static Severity minimalAlertSeverity = Severity.WARNING;
	
	/** {@link Game} that is currently being inspected*/
	public static Game game = null;//Game.CK2;
	/** {@link List} of {@link Alert}s that were encountered in program execution so far.*/
    public static final List<Alert> alerts = Collections.synchronizedList(new ArrayList<>());
    /**Currently loaded {@link List} of {@link Mod}s*/
	public static ModList mods;
 
	/**{@link Path} to the users "My Documents" folder, or equivalent*/
	public static Path docsFolder;
	/**{@link Path} to the folder where Steam is installed, if at all*/
	public static Path steamRoot;
	/**{@link Path} to the *parent* of the folder where the current game is installed. <br/>
	 * Usually %steamRoot%/steamApps/common/*/
	public static Path installRoot;
	
	/**Don't.*/
	private GlobalState()throws UnsupportedOperationException{throw new UnsupportedOperationException();}
	
	/**Changes the {@link Game} game global value, and logs said change iff debug is true.
	 * @param game the new value
	 */
	public static void setGame(Game game){
		log(String.format("Changing game from %s to %s", GlobalState.game, game));
		GlobalState.game = game;
	}
	
	/**Writes the given message to log.
	 * @param s message to write
	 */
	public static void log(String s){
		if(isDebug){
			//System.out.println(s);
			if(gui != null)
				gui.onDebugLog(s);
		}
	}
	
	/**Checks if {@link GlobalState#docsFolder} is valid.
	 * @return true iff docsFolder is valid.
	 */
	public static boolean isDocsFolderOk(){
		return game != null && Files.isReadable(game.getSettingsFile(docsFolder));
	}
	
	/**Checks if {@link GlobalState#steamRoot} is valid.
	 * @return true iff steamRoot is valid.
	 */
	public static boolean isSteamFolderOk(){
		return steamRoot != null && Files.exists(steamRoot.resolve(STEAM_EXE));
	}
	
	/**Checks if {@link GlobalState#installRoot} is valid.
	 * @return true iff installRoot is valid.
	 */
	public static boolean isGameFolderOk(){
		return game != null && installRoot != null && Files.isReadable(game.getGameDataFolder(installRoot).resolve(game.getExecutable()));
	}
}
