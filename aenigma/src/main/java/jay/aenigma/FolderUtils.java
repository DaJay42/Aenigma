package jay.aenigma;

import javax.swing.filechooser.FileSystemView;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Non-instantiable class holding constants and utility functions for Folder and File manipulation.
 */
public final class FolderUtils{
	
	private static final String STEAM_APPS_COMMON = "SteamApps/common";
	private static final String REG_QUERY_STEAM_PATH = "reg query \"HKEY_CURRENT_USER\\SOFTWARE\\Valve\\Steam\" /v SteamPath";
	
	/**Pattern that matches txt files*/
	public static final Pattern txtPattern = Pattern.compile("\\.txt$");
	/**Pattern that matches gfx and gui files*/
	public static final Pattern gfxPattern = Pattern.compile("\\.gfx$|\\.gui$");
	/**Pattern that matches csv files*/
	public static final Pattern csvPattern = Pattern.compile("\\.csv$");
	/**Pattern that matches lua files*/
	public static final Pattern luaPattern = Pattern.compile("\\.lua$");
	
	private FolderUtils(){throw new UnsupportedOperationException();}
	
	/**Retrieves a {@link Game}'s install root from its Manifest file.
	 * <p/>TODO: actually do that. currently just returns %{@link GlobalState#steamRoot}%/SteamApps/common
	 * @param manifest {@link Path} to manifest file
	 */
	public static void parseInstallRoot(Path manifest){
		//TODO: other install depots depending on manifest
		GlobalState.installRoot = GlobalState.steamRoot.resolve(STEAM_APPS_COMMON);
	}
	
	/**Creates a {@link Stream} of {@link Path}s representing all {@link java.io.File}s that are
	 * direct children of the given Path, and whose File Names match the given Pattern.
	 * <p/>
	 * It is recommended that this method be called in the resource declaration of a try-resource block
	 * in order to guarantee that all created resources are freed properly.
	 * @param dir Path of directory to search
	 * @param pattern Pattern to match File Names against
	 * @return Stream of Paths to Files
	 * @throws IOException iff walking the given Path fails
	 */
	static Stream<Path> getFilesInDir(Path dir, Pattern pattern) throws IOException{
    	final Predicate<String> predicate = pattern.asPredicate();
		return Files.walk(dir,1)
				.filter(Files::isRegularFile)
				.filter(path -> predicate.test(path.getFileName().toString()));
	}
	
	/**
	 * Resets {@link GlobalState#docsFolder} to its default value.
	 */
	public static void resetDocsFolder(){
		// theoretically, this just calls "System.getProperty("user.home")",
		// but it seems to do some black magic along the way,
		// which results in the actual documents folder being returned... ￣\_(ツ)_/￣
		GlobalState.docsFolder = FileSystemView.getFileSystemView().getDefaultDirectory().toPath();
		GlobalState.log("DefaultDirectory is " + GlobalState.docsFolder);
	}
	
	/**
	 * Resets {@link GlobalState#steamRoot} to its default value.
	 * <p/>TODO: for OS other than Windows.
	 */
	public static void resetSteamRoot(){
		try{
			String osName = System.getProperty("os.name");
			GlobalState.log("os.name is "+ osName);
			if(osName.contains("Windows") || osName.contains("NT")){
				GlobalState.log(String.format("executing: \"%s\"", REG_QUERY_STEAM_PATH));
				Process process = Runtime.getRuntime().exec(REG_QUERY_STEAM_PATH);
				InputStream inputStream = process.getInputStream();
				process.waitFor();
				int available = inputStream.available();
				byte[] bytes = new byte[available];
				int read = inputStream.read(bytes);
				assert available == read;
				inputStream.close();
				String out = new String(bytes);
				GlobalState.log(String.format("query result is: \"%s\"", out));
				GlobalState.steamRoot = Paths.get(out.split("\\s+",5)[4].trim());
			}
			//TODO: other os
			else {
				GlobalState.steamRoot = null;
			}
		}catch(Exception e){
			GlobalState.steamRoot = null;
			e.printStackTrace();
		}
	}
}
