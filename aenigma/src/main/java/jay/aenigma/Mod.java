package jay.aenigma;

import jay.aenigma.ckii.parser.*;

import org.antlr.v4.runtime.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Class that represents information about a single Mod in a {@link ModList}, including any {@link ModFile}s
 * associated with it.
 */
public class Mod implements Closeable, AutoCloseable{
	// TODO: clean up parsing, then either make generic enough to work for all games, or create subclasses
	private final String name;
	private final Path path;
	private final boolean zipped;
	private final List<String> dependencies;
	
	/**Creates a Mod with the given parameters, usually retrieved from a .mod file
	 * @param name the name of the Mod, as listed in its .mod file.
	 * @param path the {@link Path} where the Mod's files are stored;
	 *                either relative to {@link Game#getGameDataFolder(Path)} or as an absolute Path.
	 * @param zipped true if the path parameter represents a zip file, false if it represents an ordinary folder.
	 * @param dependencies {@link List} of {@link String}s representing the names of other Mods this one depends on,
	 *                                 which may or may not be loaded.
	 * @throws IOException iff zipped is true and path is not a valid archive.
	 */
	Mod(String name, Path path, boolean zipped, List<String> dependencies) throws IOException{
		this.name = name;
		this.path = path;
		this.zipped = zipped;
		this.dependencies = dependencies;
		
		if(zipped){
			zipFileSystem = FileSystems.newFileSystem(path, null);
			rootPath = zipFileSystem.getRootDirectories().iterator().next(); // if there's zip files with multiple roots, I'll flip a table
		}
		else{
			zipFileSystem = null;
			rootPath = path;
		}
	}
	
	private final Set<ModFile> files = new HashSet<>();
	private final Map<String, ModFile> modFileMap = new HashMap<>();
	private final List<Definition> definitions = new ArrayList<>();
	private final FileSystem zipFileSystem;
	private final Path rootPath;
	
	/**
	 * Represents a single file in a loaded {@link Mod}.
	 */
	public class ModFile{
		final Path relativePath;
		final GameFolder gameFolder;
		final String name;
		final List<Definition> definitions;
		
		/**Creates a new ModFile representing a file at the given relative {@link Path}
		 *  of the given {@link GameFolder}.
		 * @param relativePath {@link Path} relative to this {@link Mod}'s root folder at {@link Mod#getPath()}
		 * @param gameFolder GameFolder that this file is part of
		 */
		ModFile(Path relativePath, GameFolder gameFolder){
			this.relativePath = relativePath;
			this.gameFolder = gameFolder;
			this.name = relativePath.toString().replace('/','\\');
			this.definitions = new ArrayList<>();
		}
		
		/**Retrieve the {@link Mod} that this ModFile is part of.
		 * @return Mod that this file is part of
		 */
		public Mod getMod(){
			return Mod.this;
		}
		
		/**Retrieves this ModFile's relative {@link Path} as a standardized {@link String}
		 * @return this file's relative path as a standardized String
		 */
		public String getName(){
			return name;
		}
		
		/**Retrieves this ModFile's {@link Path} relative to its {@link Mod}s root {@link Mod#getPath()}.
		 * @return Path relative to Mod root Path.
		 */
		public Path getRelativePath(){
			return relativePath;
		}
		
		/**Get {@link GameFolder} that this MddFile belongs to.
		 * @return GameFolder that this ModFile belongs to.
		 */
		public GameFolder getGameFolder(){
			return gameFolder;
		}
		
		/**Absolute {@link Path} to this ModFile, obtained by resolving its relative Path from its {@link Mod}'s
		 * root {@link Mod#getPath()}. If the Mod is zipped, it will point inside a zipFileSystem, in which case it no longer
		 * valid once the Mod is {@link Mod#close()}d.
		 * @return Absolute Path to this ModFile
		 */
		public Path getAbsolutePath(){
			return getMod().rootPath.resolve(relativePath);
		}
		
		public List<Definition> getDefinitions(){
			return Collections.unmodifiableList(definitions);
		}
		
	}
	
	/**Retrieves the {@link ModFile} in this Mod whose relative {@link Path} is represented by the given {@link String},
	 * if such a ModFile exists, and null otherwise.
	 * @param name String representation of the relative Path of the desired ModFile.
	 * @return the ModFile at the given Path, if it exists, null otherwise.
	 */
	public ModFile getModFileByName(String name){
		return modFileMap.get(name);
	}
	
	
	/**Populate the {@link List}s of {@link ModFile}s and {@link Definition}s in this Mod by parsing all relevant files.
	 * @return a List of {@link Alert}s describing all parsing errors encountered.
	 */
	List<Alert> parseFiles(){
		long time = System.nanoTime();
		List<Alert> alerts = new ArrayList<>();
		try{
			for(GameFolder gameFolder : GlobalState.game.getGameFolders()){
				Path folder = rootPath.resolve(gameFolder.getPath());
				
				if(Files.exists(folder) && Files.isReadable(folder)){
					try(Stream<Path> pathStream = FolderUtils.getFilesInDir(folder, gameFolder.getFileNameRegex())){
						pathStream.sequential().forEach(path -> alerts.addAll(parseFile(path, gameFolder)));
					}
				}
			}
		} catch(IOException e){
			e.printStackTrace();
		}
		time = System.nanoTime() - time;
		time /= 1e6;
		GlobalState.log(String.format("Parsed files for \"%s\" (%d ms)", this.getName(), time));
		return alerts;
	}
	
	/**Generate the {@link List} of {@link Definition}s defined in the {@link File} at the given {@link Path},
	 * by parsing said file using a matching {@link Parser}. The resulting {@link ModFile} and any encountered
	 * Definitions are added to their respective Lists and {@link Map}s in this Mod.
	 * @param path the (absolute) Path of the File to be parsed
	 * @param gameFolder the GameFolder of the File to be parsed
	 * @return a List of {@link Alert}s describing all parsing errors encountered.
	 */
	private List<Alert> parseFile(Path path, GameFolder gameFolder){
		try{
			final Path relativePath = rootPath.relativize(path);
			final ModFile modFile = new ModFile(relativePath, gameFolder);
			files.add(modFile);
			modFileMap.put(modFile.getName(),modFile);
			//Main.log("\tparsing "+path.getFileName());

			Pattern pattern = gameFolder.getReservedNames();
			Predicate<String> badNamePredicate = pattern != null ? pattern.asPredicate() : String::isEmpty;
			
			AlertErrorListener listener = new AlertErrorListener(modFile);
			
			if(path.getFileName().toString().toLowerCase().endsWith(".csv")){
				CkiiLocLexer lexer = new CkiiLocLexer(CharStreams.fromPath(path, gameFolder.getCharset()));
				lexer.removeErrorListeners();
				lexer.addErrorListener(listener);
				CkiiLocParser parser = new CkiiLocParser(new CommonTokenStream(lexer));
				parser.setErrorHandler(new DefaultErrorStrategy());
				parser.removeErrorListeners();
				parser.addErrorListener(listener);
				CkiiLocParser.UnitContext unit = parser.unit();
				
				LocKeyVisitor locKeyVisitor = new LocKeyVisitor();
				List<String> stringList = locKeyVisitor.visit(unit);
				for(String string : stringList){
					if(!badNamePredicate.test(string))
						modFile.definitions.add(new Definition(string, modFile, gameFolder.getMergeBehaviour()));
				}
			}
			else {
				CkiiLexer lexer = new CkiiLexer(CharStreams.fromPath(path, gameFolder.getCharset()));
				lexer.removeErrorListeners();
				lexer.addErrorListener(listener);
				CkiiParser parser = new CkiiParser(new CommonTokenStream(lexer));
				parser.setErrorHandler(new DefaultErrorStrategy());
				parser.removeErrorListeners();
				parser.addErrorListener(listener);
				CkiiParser.UnitContext unit = parser.unit();
				
				switch(gameFolder.getNamingType()){
					case TOP_LEVEL:
						TopLevelNameVisitor topLevelNameVisitor = new TopLevelNameVisitor();
						List<String> strings = topLevelNameVisitor.visit(unit);
						for(String string : strings){
							if(!badNamePredicate.test(string))
								modFile.definitions.add(new Definition(string, modFile, gameFolder.getMergeBehaviour()));
						}
						break;
					case SECOND_LEVEL:
						MergeBehaviour mergeBehaviour = gameFolder.getMergeBehaviour();
						if(mergeBehaviour == MergeBehaviour.MERGE_GROUPS){
							topLevelNameVisitor = new TopLevelNameVisitor();
							List<String> strings1 = topLevelNameVisitor.visit(unit);
							for(String string : strings1){
								if(!badNamePredicate.test(string))
									modFile.definitions.add(new Definition(string, modFile, mergeBehaviour));
							}
							mergeBehaviour = MergeBehaviour.REPLACE;
						}
						SecondLevelNameVisitor secondLevelNameVisitor = new SecondLevelNameVisitor();
						List<String> list = secondLevelNameVisitor.visit(unit);
						for(String string : list){
							if(!badNamePredicate.test(string))
								modFile.definitions.add(new Definition(string, modFile, mergeBehaviour));
						}
						break;
					case PREFIX_ANY_LEVEL:
						TitleNameVisitor titleNameVisitor = new TitleNameVisitor();
						List<String> stringList = titleNameVisitor.visit(unit);
						for(String string : stringList){
							if(!badNamePredicate.test(string))
								modFile.definitions.add(new Definition(string, modFile, gameFolder.getMergeBehaviour()));
						}
						break;
					case ID_FIELD:
						TopLevelIdFieldNameVisitor topLevelIdFieldNameVisitor = new TopLevelIdFieldNameVisitor();
						topLevelIdFieldNameVisitor.target = gameFolder.getIdField();
						List<String> visit = topLevelIdFieldNameVisitor.visit(unit);
						for(String string : visit){
							if(!badNamePredicate.test(string))
								modFile.definitions.add(new Definition(string, modFile, gameFolder.getMergeBehaviour()));
						}
						break;
					case SECOND_LEVEL_ID_FIELD:
						SecondLevelIdFieldNameVisitor secondLevelIdFieldNameVisitor = new SecondLevelIdFieldNameVisitor();
						secondLevelIdFieldNameVisitor.target = gameFolder.getIdField();
						List<String> visit1 = secondLevelIdFieldNameVisitor.visit(unit);
						for(String string : visit1){
							if(!badNamePredicate.test(string))
								modFile.definitions.add(new Definition(string, modFile, gameFolder.getMergeBehaviour()));
						}
						break;
					case ANY_LEVEL_ID_FIELD:
						AnyLevelIdFieldNameVisitor anyLevelIdFieldNameVisitor = new AnyLevelIdFieldNameVisitor();
						anyLevelIdFieldNameVisitor.target = gameFolder.getIdField();
						List<String> strings1 = anyLevelIdFieldNameVisitor.visit(unit);
						for(String string : strings1){
							if(!badNamePredicate.test(string))
								modFile.definitions.add(new Definition(string, modFile, gameFolder.getMergeBehaviour()));
						}
						break;
				}
			}
			definitions.addAll(modFile.definitions);
			return listener.getAlerts();
		}catch(Exception e){
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	/**Creates a new {@link Mod} instance by parsing the *.mod {@link File} at the relative {@link Path}
	 * represented by modFile.
	 * @param modFile {@link String} representation of the relative Path of the *.mod file to be loaded.
	 * @return a new Mod instance representing the contents of the File at the given Path.
	 * @throws IOException iff loading the File at the given Path fails for any reason.
	 */
	static Mod load(String modFile) throws IOException{
		Path userDataFolder = GlobalState.game.getUserDataFolder(GlobalState.docsFolder);
		Path path = userDataFolder.resolve(modFile);
		File file = path.toFile();
	
		FileReader fileReader = new FileReader(file);
		CkiiLexer lexer = new CkiiLexer(CharStreams.fromReader(fileReader));
		CkiiParser parser = new CkiiParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new DefaultErrorStrategy());
		CkiiParser.UnitContext unit = parser.unit();
		
		ModFileVisitor visitor = new ModFileVisitor();
		List<String> visit = visitor.visit(unit);
		
		return new Mod(visitor.getName(), userDataFolder.resolve(visitor.getPath()), visitor.isZipped(), visit);
	}
	
	/**Retrieves the Name of this Mod, as defined in its *.mod File.
	 * @return the Name of the Mod
	 */
	public String getName(){
		return name;
	}
	
	/**Retrieves the root {@link Path} of this Mod. <br/>
	 * If this Mod {@link Mod#isZipped()}, then the returned Path points inside a zip {@link java.nio.file.FileSystem}
	 * and will become invalid once this Mod is {@link Mod#close()}d.
	 * @return the root Path of this mod
	 */
	public Path getPath(){
		return path;
	}
	
	/**Returns true iff this Mod's Files reside inside a zip archive. <br/>
	 * If true, then {@link Mod#close()}ing this Mod will
	 * close the {@link java.nio.file.FileSystem} representing the contents of the archive,
	 * invalidating any {@link Path}s to {@link ModFile}s in this Mod retained elsewhere.
	 * @return true iff this Mod's Files reside inside a zip archive.
	 */
	public boolean isZipped(){
		return zipped;
	}
	
	/**Retrieves a {@link List} of {@link String}s representing the Names of all Mods that this Mod depends on,
	 * as listed in its *.mod File. <br/>
	 * Note that this is not a complete List of dependencies and that not all Mods listed may actually be loaded in a
	 * given {@link ModList}.
	 * @see ModList#getTrueDependencies(Mod)
	 * @return List of Strings representing this Mod's listed dependencies
	 */
	public List<String> getDependencies(){
		return dependencies;
	}
	
	/**Retrieves the {@link Set} of {@link ModFile}s belonging to this Mod,
	 * provided {@link Mod#parseFiles()} was called on this Mod previously.
	 * @return Set of ModFiles belonging to this Mod
	 */
	public Set<ModFile> getFiles(){
		return files;
	}
	
	/**Retrieves the {@link List} of {@link Definition}s belonging to this Mod,
	 * provided {@link Mod#parseFiles()} was called on this Mod previously.
	 * @return List of Definitions belonging to this Mod
	 */
	public List<Definition> getDefinitions(){
		return definitions;
	}
	
	/**Closes this Mod. If this Mod {@link Mod#isZipped()}, then the {@link java.nio.file.FileSystem}
	 * representing the Mod Archive's contents is closed, rendering any {@link Path} to {@link ModFile}s within invalid.
	 * @see java.nio.file.FileSystem#close()
	 * @throws IOException iff closing the FileSystem throws any
	 */
	@Override
	public void close() throws IOException{
		if(zipFileSystem != null){
			zipFileSystem.close();
		}
	}
}
