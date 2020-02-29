package jay.aenigma;

import jay.aenigma.ckii.parser.CkiiLexer;
import jay.aenigma.ckii.parser.CkiiParser;
import jay.aenigma.ckii.parser.CkiiVisitor;
import jay.aenigma.ckii.parser.ModListVisitor;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;

/**
 * Class that represents the list of currently loaded mods. <br/>
 * Contains functions that operate on the entire list, as well as data that depends on having an entire list. <br/>
 * The contained instances of {@link Mod} can be retrieved with {@link ModList#getValues()}.
 */
public class ModList implements Closeable{
	
	
	private final List<Mod> mods;
	private Mod vanilla;
	private Map<String, List<Mod>> fileToModsMap;
	private Map<String, List<Definition>> definitionsMap;
	private Map<Mod, Set<Mod>> trueDependencies;
	private Map<Mod, Set<Mod>> dependerMap;
	private Set<Mod.ModFile> unShadowedFiles;
	private Set<Definition> unShadowedDefinitions;
	private Map<Mod.ModFile, Set<Mod.ModFile>> shadowingFiles;
	private Map<Definition, Set<Definition>> shadowingDefinitions;
	
	/** Creates a new {@link ModList} containing the {@link Mod}s referenced by the Settings File at the given Path.
	 * @see ModList#getMods(Path)
	 * @param settingsFile Path to the Settings file that contains the list of mods.
	 * @throws IOException if reading the Settings File fails for any reason.
	 */
	public ModList(Path settingsFile) throws IOException{
		mods = getMods(settingsFile);
	}
	
	private void setTrueDependencies(Mod dependerMod, Collection<Mod> dependeeMods){
		trueDependencies.put(dependerMod, Set.copyOf(dependeeMods));
	}
	
	/** Returns an unmodifiable {@link Set} containing all direct and indirect dependencies of
	 * the given {@link Mod} that are in this {@link ModList}.
	 * @param dependerMod the Mod whose dependencies are to be retrieved.
	 * @return unmodifiable Set of dependencies of dependerMod
	 */
	Set<Mod> getTrueDependencies(Mod dependerMod){
		return trueDependencies.get(dependerMod);
	}
	
	/** Returns true iff the given {@link Mod} dependeeMod is a direct or indirect dependency of dependerMod,
	 * in the context of this ModList.
	 * @param dependerMod Mod that should depend
	 * @param dependeeMod Mod that should be depended on
	 * @return true iff the dependency exists
	 */
	boolean isTrueDependency(Mod dependerMod, Mod dependeeMod){
		return trueDependencies.containsKey(dependerMod) && trueDependencies.get(dependerMod).contains(dependeeMod);
	}
	
	void computeDependerMap(){
		dependerMap = new HashMap<>(mods.size());
		for(Mod mod : mods){
			for(Mod mod1 : mods){
				if(mod != mod1 && isTrueDependency(mod1, mod)){
					if(!dependerMap.containsKey(mod)){
						dependerMap.put(mod, new HashSet<>());
					}
					dependerMap.get(mod).add(mod1);
				}
			}
		}
	}
	
	void computeFileShadowing(){
		unShadowedFiles = new HashSet<>();
		shadowingFiles = new HashMap<>();
		for(Mod mod : mods){
			for(Mod.ModFile file : mod.getFiles()){
				if(!dependerMap.containsKey(file.getMod())){
					unShadowedFiles.add(file);
				} else{
					Set<Mod.ModFile> fileSet = dependerMap.get(file.getMod()).stream()
							.map(mod1 -> mod1.getModFileByName(file.name))
							.filter(Objects::nonNull)
							.collect(Collectors.toSet());
					if(fileSet.isEmpty()){
						unShadowedFiles.add(file);
					} else {
						shadowingFiles.put(file, fileSet);
					}
				}
			}
		}
	}
	
	List<Alert> computeDefinitionShadowing(){
		List<Alert> alerts = new ArrayList<>();
		unShadowedDefinitions = new HashSet<>();
		shadowingDefinitions = new HashMap<>();
		for(Mod mod : mods){
			if(dependerMap.containsKey(mod)){
				Set<Mod> mods = dependerMap.get(mod);
				for(Definition definition : mod.getDefinitions()){
					Set<Mod.ModFile> fileSet = shadowingFiles.get(definition.getFile());
					if(fileSet == null || fileSet.isEmpty()){
						unShadowedDefinitions.add(definition);
					}
					else{
						Set<Definition> definitionSet = mods.stream()
								.flatMap(mod1 -> mod1.getDefinitions().stream())
								.filter(definition::isEquivalent)
								.collect(Collectors.toSet());
						if(definitionSet.isEmpty()){
							List<Mod.ModFile> modFiles = new ArrayList<>(fileSet.size()+1);
							modFiles.add(definition.getFile());
							modFiles.addAll(fileSet);
							alerts.add(new Alert(Severity.WARNING,
									Alert.Kind.UNDEFINE,
									definition.getGameFolder(),
									modFiles,
									definition.getName(),
									String.format("An instance of type %s\n with name \"%s\"\n was deleted in some overwriting file(s).",
											definition.getGameFolder().toString(), definition.getName())
							));
						}
						else {
							shadowingDefinitions.put(definition, definitionSet);
						}
					}
				}
			}
			else {
				unShadowedDefinitions.addAll(mod.getDefinitions());
			}
		}
		return alerts;
	}
	
	/** Retrieves the unmodifiable {@link List} of {@link Mod}s represented by this ModList.
	 * @return the unmodifiable List
	 */
	public List<Mod> getValues(){
		return mods;
	}
	
	/** Performs all steps necessary to calculate all conflicts withing this ModList. <br/>
	 * Returns a {@link List} containing an {@link Alert} for each conflict or parse error encountered.
	 * @return List of all Alerts encountered.
	 */
	public List<Alert> runConflictCheck(){
		List<Alert> alerts = new ArrayList<>();
		resolveTrueDependencies();
		computeDependerMap();
		alerts.addAll(parseAllModFiles());
		fileToModsMap = getFileToModsMap();
		GlobalState.log("");
		GlobalState.log("Checking for shadowed files...");
		computeFileShadowing();
		GlobalState.log("");
		GlobalState.log("Checking for shadowed definitions...");
		alerts.addAll(computeDefinitionShadowing());
		GlobalState.log("");
		GlobalState.log("Mapping definitions...");
		definitionsMap = getDefinitionsMap();
		GlobalState.log("");
		GlobalState.log("Checking for conflicts...");
		GlobalState.log("");
		alerts.addAll(findFileNameConflicts());
		GlobalState.log("");
		alerts.addAll(findDefinitionConflicts());
		return alerts;
	}
	
	/**Loads all {@link Mod}s described in the Settings File at the given {@link Path}. <br/>
	 * Returns an unmodifiable {@link List} containing the resulting mods.
	 * @param settingsFile Path to a Settings File
	 * @return unmodifiable List of Mods that were loaded.
	 * @throws IOException iff either parsing the Settings File or any of the Mod Files throws one
	 */
	private List<Mod> getMods(Path settingsFile) throws IOException{
		List<String> modPathList = getModPathList(settingsFile);
		GlobalState.log("Checking mods...");
		
		List<Mod> mods = new ArrayList<>();
		
		vanilla = new Mod("(Vanilla) " + GlobalState.game.toString(),
				GlobalState.game.getGameDataFolder(GlobalState.installRoot),
				false, new ArrayList<>()
		);
		
		mods.add(vanilla);
		
		for(String modPath : modPathList){
			String modFile = modPath.substring(1, modPath.length() - 1);
			GlobalState.log("reading file " + modFile);
			Mod mod = Mod.load(modFile);
			mod.getDependencies().add(vanilla.getName());
			mods.add(mod);
		}
		GlobalState.log("");
		return List.copyOf(mods);
	}
	
	/** Parses all {@link jay.aenigma.Mod.ModFile}s for each {@link Mod} in this {@link List},
	 * by using the {@link GlobalState#backgroundExecutor} to invoke {@link Mod#parseFiles()} on each of them.
	 * @return the List of {@link Alert}s generated by Parsing errors.
	 */
	private List<Alert> parseAllModFiles(){
		List<Alert> alerts = new ArrayList<>();
		try{
			GlobalState.log("Parsing all mod files.");
			List<Future<List<Alert>>> futures = GlobalState.backgroundExecutor.invokeAll(
					mods.stream()
							.map(mod -> (Callable<List<Alert>>) mod::parseFiles)
							.collect(Collectors.toList())
			);
			for(Future<List<Alert>> listFuture : futures){
				alerts.addAll(listFuture.get());
			}
			GlobalState.log("Parsed all mod files.");
		} catch(InterruptedException | ExecutionException e){
			e.printStackTrace();
		}
		return alerts;
	}
	
	/** Generates the {@link List} of {@link Alert}s corresponding to all File Name Conflicts that occur
	 * between any non-dependent {@link Mod}s in this List.
	 * @return The List of Alerts generated by File Name Conflicts.
	 */
	private List<Alert> findFileNameConflicts(){
		GlobalState.log("Checking for file name conflicts...");
		List<Alert> alerts = new ArrayList<>();
		for(Map.Entry<String, List<Mod>> entry : fileToModsMap.entrySet()){
			if(entry.getValue().size() > 1){
				List<Mod> conflicts = cleanConflictList(entry.getValue());
				if(conflicts.size() > 2 || (!conflicts.contains(vanilla) && conflicts.size() > 1)){
					List<Mod.ModFile> modFiles = conflicts.stream()
							.map(mod -> mod.getModFileByName(entry.getKey()))
							.collect(Collectors.toUnmodifiableList());
					GameFolder gameFolder = modFiles.get(0).getGameFolder();
					Alert alert = new Alert(Severity.WARNING, Alert.Kind.FILE_CONFLICT, gameFolder, modFiles,
							null, "File name conflict between non-dependent mods.");
					//GlobalState.log(alert.toString());
					alerts.add(alert);
				}
			}
		}
		GlobalState.log("Got file name conflicts.");
		return alerts;
	}
	
	/** Generates the {@link List} of {@link Alert}s corresponding to all {@link Definition} conflicts that occur
	 * in this ModList, that is, all Lists of Definitions that are each equal in {@link Definition#getName()} and {@link Definition#getGameFolder()},
	 * but distinct in {@link Definition#getFile()}.
	 * @return The List of Alerts corresponding to {@link Definition} conflicts
	 */
	private List<Alert> findDefinitionConflicts(){
		GlobalState.log("Checking for definition conflicts...");
		List<Alert> alerts = new ArrayList<>();
		for(Map.Entry<String, List<Definition>> entry : definitionsMap.entrySet()){
			String definitionName = entry.getKey();
			List<Definition> duplicateDefinitions = entry.getValue();
			if(duplicateDefinitions.size() > 1){
				Set<GameFolder> gameFolders = duplicateDefinitions.stream().map(Definition::getGameFolder)
						.collect(Collectors.toUnmodifiableSet());
				for(GameFolder gameFolder : gameFolders){
					List<Definition> duplicateDefinitionsInGameFolderList = duplicateDefinitions.stream()
							.filter(definition -> definition.getGameFolder().equals(gameFolder))
							.collect(Collectors.toList());
					if(duplicateDefinitionsInGameFolderList.size() > 1
							&& duplicateDefinitionsInGameFolderList.stream().map(Definition::getFile).map(Mod.ModFile::getName).distinct().count() > 1){
						MergeBehaviour mergeBehaviour = duplicateDefinitionsInGameFolderList.get(0).getMergeBehaviour();
						if(mergeBehaviour.compareTo(MergeBehaviour.NOT_APPLICABLE) > 0){
							String name = duplicateDefinitionsInGameFolderList.get(0).getName();
							List<Mod.ModFile> modFiles = duplicateDefinitionsInGameFolderList.stream().map(Definition::getFile).collect(Collectors.toList());
							Alert alert = new Alert(mergeBehaviour.severity, Alert.Kind.NAME_CONFLICT,
									gameFolder, modFiles, name,
									"An instance of type "+gameFolder.toString() +"\n" +
											" with name \""+ definitionName + "\"\n" +
											" is defined in multiple distinctly-named files.\n"
											+ mergeBehaviour.description);
							alerts.add(alert);
						}
					}
				}
			}
		}
		GlobalState.log("Got definition conflicts.");
		return alerts;
	}
	
	/**Generates a {@link Map} relating each {@link Mod.ModFile#getName()} to a {@link List} of {@link Mod.ModFile#getMod()}
	 * for each loaded {@link jay.aenigma.Mod.ModFile} in this List of {@link Mod}s.
	 * @return a multi-map of File Names to Mods
	 */
	private Map<String, List<Mod>> getFileToModsMap(){
		Map<String, List<Mod>> stringModListMap = new HashMap<>();
		for(Mod mod : mods){
			for(Mod.ModFile file : mod.getFiles()){
				if(!stringModListMap.containsKey(file.getName())){
					stringModListMap.put(file.getName(), new ArrayList<>());
				}
				stringModListMap.get(file.getName()).add(mod);
			}
		}
		return stringModListMap;
	}
	
	/**Prevents compatibility patches and similar {@link Mod}s from being recognized as conflicts
	 * by taking a {@link List} of conflicting Mods and removing any that are dependencies of another Mod in said List.
	 * @param conflictedMods the List of conflicting Mods to be filtered.
	 * @return the filtered List of Mods
	 */
	private List<Mod> cleanConflictList(List<Mod> conflictedMods){
    	int n = conflictedMods.size();
    	boolean[] clean = new boolean[n];
		for(int i = 0; i < n; i++){
			if(!conflictedMods.get(i).equals(vanilla)){
				for(int j = 0; j < n; j++){
					if(i != j && isTrueDependency(conflictedMods.get(j), conflictedMods.get(i))){
						clean[i] = true;
					}
				}
			}
		}
		List<Mod> dirtyMods = new ArrayList<>();
		for(int i = 0; i < n; i++){
			if(!clean[i])
				dirtyMods.add(conflictedMods.get(i));
		}
		return dirtyMods;
	}
	
	/**Generates the {@link Map} {@link ModList#trueDependencies}, which relates each {@link Mod} in this ModList
	 * to all of its direct or indirect dependencies among the Mods loaded in this ModList by means of a
	 * breadth-first search.<br/>
	 * I.e. if we view the dependency relation between Mods as a Directed Acyclic Graph, then this method
	 * maps every node in said DAG to the {@link Set} of nodes that are reachable from it, not including the node itself.
	 */
	private void resolveTrueDependencies(){
		GlobalState.log("Resolving dependencies...");
		trueDependencies = new HashMap<>();
		Map<String, Mod> modsByNameMap = Util.mapBy(mods, Mod::getName);
		
		for(Mod dependerMod : mods){
			Queue<Mod> dependeeModQueue = new ArrayDeque<>();
			Set<Mod> encounteredModSet = new HashSet<>();
			dependeeModQueue.add(dependerMod);
			while(!dependeeModQueue.isEmpty()){
				Mod nextMod = dependeeModQueue.poll();
				for(String dependency : nextMod.getDependencies()){
					if(modsByNameMap.containsKey(dependency)){
						Mod dependeeMod = modsByNameMap.get(dependency);
						if(!encounteredModSet.contains(dependeeMod)){
							encounteredModSet.add(dependeeMod);
							dependeeModQueue.add(dependeeMod);
						}
					}
				}
			}
			setTrueDependencies(dependerMod, encounteredModSet);
		}
		GlobalState.log("Resolved dependencies.");
	}
	
	/**Retrieve the {@link List} of {@link String}s representing the {@link Path}s to each {@link Mod} listed in the
	 * Settings File at the given Path.
	 * @param settingsFile Path where the Settings File is
	 * @return the List of Strings describing the Path to each Mod
	 * @throws IOException iff parsing the Settings File fails.
	 */
	private List<String> getModPathList(Path settingsFile) throws IOException{
		GlobalState.log("Parsing settings file...");
		File file = settingsFile.toFile();
		FileReader fileReader = new FileReader(file);
		CkiiLexer lexer = new CkiiLexer(CharStreams.fromReader(fileReader));
		CkiiParser parser = new CkiiParser(new CommonTokenStream(lexer));
		parser.setErrorHandler(new DefaultErrorStrategy());
		CkiiParser.UnitContext unit = parser.unit();
		
		CkiiVisitor<List<String>> visitor = new ModListVisitor();
		List<String> visit = visitor.visit(unit);
		GlobalState.log("Parsed settings file.");
		return visit;
	}
	
	/**Generates a {@link Map} that, for each {@link Definition} in the {@link Mod}s loaded in this ModList,
	 * relates its Name {@link Definition#getName()} to all Definitions with equal Name.
	 * @return The Mapping of Names to {@link List}s of Definition.
	 */
	private Map<String, List<Definition>> getDefinitionsMap(){
		return Util.multiMapBy(unShadowedDefinitions, Definition::getName);
	}
	
	/**Closes all {@link Mod}s in this List.
	 * @throws IOException iff any {@link Mod#close()} throws
	 * @see Mod#close()
	 */
	@Override
	public void close() throws IOException{
		if(mods != null)
			for(Mod mod : mods)
				mod.close();
	}
}
