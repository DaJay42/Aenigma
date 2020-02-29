package jay.aenigma.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import jay.aenigma.*;
import jay.aenigma.Alert;
import jay.aenigma.ckii.TreeNode;

import org.antlr.v4.runtime.CharStreams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Gui extends Application{
	
	private static final String MOD_LIST_DISABLED = "Game, Documents folder and Game Data folder must be set for mod list to be loaded.";
	private static final String INSERT = "INSERT";
	private static final String MODIFY = "MODIFY";
	private static final String DELETE = "DELETE";
	
	private TextArea debugArea = new TextArea();
	private ChoiceBox<Game> gameChoiceBox;
	private Label settingsStateLabel;
	@SuppressWarnings("FieldCanBeLocal")
	private TextField docPathField;
	private Label gameStateLabel;
	private TextField gamePathField;
	private TableView<Mod> modTableView;
	private TableView<Alert> alertTableView;
	private Button checkConflictsButton;
	private Stage primaryStage;
	
	@Override
	public void start(Stage primaryStage){
		GlobalState.gui = this;
		this.primaryStage = primaryStage;
		primaryStage.setTitle("Aenigma: a PGS mod conflict detector");
		
		gameChoiceBox = new ChoiceBox<>();
		gameChoiceBox.getItems().addAll(Game.values());
		//gameChoiceBox.setValue(Game.CK2);
		gameChoiceBox.setOnAction(event -> {
			GlobalState.setGame(gameChoiceBox.getValue());
			tryUpdateInstallRoot();
			onUpdateDocsFolder(GlobalState.docsFolder);
			onUpdateGamePath(GlobalState.game.getGameDataFolder(GlobalState.installRoot));
		});
		HBox gameHBox = new HBox(4, new Label("Choose Game:"), gameChoiceBox);
		
		checkConflictsButton = new Button("Check for Conflicts.");
		checkConflictsButton.setOnAction(event -> runConflictCheck());
		checkConflictsButton.setDisable(true);
		
		/// define the mod list table (populated later)
		
		modTableView = new TableView<>();
		TableColumn<Mod, String> modNameColumn = new TableColumn<>("Name");
		modNameColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getName()));
		modNameColumn.setPrefWidth(320);
		modTableView.getColumns().add(modNameColumn);
		TableColumn<Mod, String> modPathColumn = new TableColumn<>("Path");
		modPathColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getPath().toString()));
		modPathColumn.setPrefWidth(560);
		modTableView.getColumns().add(modPathColumn);
		TableColumn<Mod, String> modZipColumn = new TableColumn<>("Zipped");
		modZipColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(Boolean.toString(param.getValue().isZipped())));
		modTableView.getColumns().add(modZipColumn);
		
		modTableView.setPlaceholder(new Label(MOD_LIST_DISABLED));
		
		// define the alert list table (populated later)
		alertTableView = makeAlertTableView();
		alertTableView.setPlaceholder(new Label("Click \"Check for Conflicts.\" to begin."));
		
		/// try to find the docs folder with settings.txt
		
		FolderUtils.resetDocsFolder();
		
		settingsStateLabel = new Label("");
		docPathField = new TextField(GlobalState.docsFolder.toString());
		docPathField.setPromptText("Please input the path to your documents folder.");
		HBox docPathBox = makeFolderNameInput("My Documents:", docPathField, this::onUpdateDocsFolder);
		onUpdateDocsFolder(GlobalState.docsFolder);
		
		/// try to find the game *.exe
		FolderUtils.resetSteamRoot();
		tryUpdateInstallRoot();
		
		gameStateLabel = new Label("");
		
		gamePathField = new TextField(GlobalState.game != null ? GlobalState.game.getGameDataFolder(GlobalState.installRoot).toString() : null);
		gamePathField.setPromptText("Please input the path to the game's install folder.");
		HBox gamePathBox = makeFolderNameInput("Game Data Folder:", gamePathField, this::onUpdateGamePath);
		if(GlobalState.game != null)
			onUpdateGamePath(GlobalState.game.getGameDataFolder(GlobalState.installRoot));
		
		
		HBox conflictsHeaderPane = new HBox(8, new Label("Conflicts"), checkConflictsButton);
		
		/// Debug area
		debugArea.setEditable(false);
		
		
		/// overall layout
		VBox vBox = new VBox(4,
				gameHBox, docPathBox, settingsStateLabel,
				new Separator(Orientation.HORIZONTAL),
				gamePathBox, gameStateLabel,
				new Separator(Orientation.HORIZONTAL),
				new Label("Active Mods"), modTableView,
				new Separator(Orientation.HORIZONTAL),
				conflictsHeaderPane, alertTableView
		);
		vBox.setPadding(new Insets(4));
		Scene scene;
		if(GlobalState.isDebug){
			HBox hBox = new HBox(4, vBox, new Separator(Orientation.VERTICAL), debugArea);
			scene = new Scene(hBox, 1280, 640);
		}
		else{
			scene = new Scene(vBox, 960, 640);
		}
		primaryStage.setScene(scene);
		primaryStage.show();
	}
	
	private List<List<String>> tryRepairFile(Mod.ModFile modFile){
		Path absolutePath = modFile.getAbsolutePath();
		List<String> elements;
		List<String> repairedElements;
		List<List<String>> lines;
		TreeNode treeNode;
		try{
			GlobalState.log(String.format("Parsing file '%s'.", modFile.getName()));
			elements = Files.readAllLines(absolutePath, modFile.getGameFolder().getCharset());
			GlobalState.log(String.format("Attempting to repair '%s'.", modFile.getName()));
			treeNode = TreeNode.valueOf(String.join("\r\n", elements));
			repairedElements = new ArrayList<>(treeNode.toStrings());
			GlobalState.log("Formatting results");
			int n = Math.max(elements.size(), repairedElements.size());
			while(elements.size() < n){
				elements.add("");
			}
			while(repairedElements.size() < n){
				elements.add("");
			}
			lines = IntStream.range(0, n).mapToObj(i ->
					List.of(String.valueOf(i), elements.get(i), repairedElements.get(i))
			).collect(Collectors.toList());
		} catch(IOException e){
			GlobalState.log(e.getLocalizedMessage());
			e.printStackTrace();
			return null;
		}
		return lines;
	}
	
	private static class TreeComparison{
		Deque<String> masterStrings;
		Deque<String> subStrings;
		Deque<Integer> masterAnnotations;
		Deque<Integer> subAnnotations;
		
		TreeComparison(Deque<String> masterStrings, Deque<String> subStrings, Deque<Integer> masterAnnotations, Deque<Integer> subAnnotations){
			this.masterStrings = masterStrings;
			this.subStrings = subStrings;
			this.masterAnnotations = masterAnnotations;
			this.subAnnotations = subAnnotations;
		}
	}
	
	private List<List<String>> compareModFiles(List<Mod.ModFile> modFiles){
		return compareModFiles(modFiles, null);
	}
	
	private List<List<String>> compareModFiles(List<Mod.ModFile> modFiles, String definitionName){
		final List<List<String>> listList = new ArrayList<>(); // rows x cols
		final int n = modFiles.size();
		try{
			// a row has the cols { Status, content } x #modfiles
			final Mod.ModFile masterFile = modFiles.get(0);
			GlobalState.log(String.format("Parsing file '%s' in '%s'.", masterFile.getName(), masterFile.getMod().getName()));
			final String master = String.join("\r\n", Files.readAllLines(masterFile.getAbsolutePath(),
					masterFile.getGameFolder().getCharset()));
			
			final List<Deque<String>> masterStringDeques = new ArrayList<>(n-1);
			final List<Deque<String>> subStringDeques = new ArrayList<>(n-1);
			final List<Deque<Integer>> masterIntegerDeques = new ArrayList<>(n-1);
			final List<Deque<Integer>> subIntegerDeques = new ArrayList<>(n-1);
			
			final List<Future<TreeComparison>> futureList = GlobalState.backgroundExecutor.invokeAll(
					IntStream.range(1, n).<Callable<TreeComparison>>mapToObj((int i) ->
						() -> {
							try{
								Mod.ModFile subFile = modFiles.get(i);
								GlobalState.log(String.format("Parsing file '%s' in '%s'.", subFile.getName(), subFile.getMod().getName()));
								TreeNode subNode = TreeNode.valueOf(CharStreams.fromPath(subFile.getAbsolutePath(),
										subFile.getGameFolder().getCharset()));
								TreeNode masterNode = TreeNode.valueOf(master);
								
								if(definitionName != null){
									String subIdField = subFile.getGameFolder().getIdField();
									String masterIdField = masterFile.getGameFolder().getIdField();
									Predicate<String> labelMatcher = TreeNode.makeLabelMatcher(definitionName);
									if(subIdField != null){
										Predicate<String> subLabelNameMatcher = TreeNode.makeLabelNameMatcher(subIdField, definitionName);
										subNode = subNode.findByNameField(subLabelNameMatcher);
									}
									else{
										subNode = subNode.findByLabel(labelMatcher);
									}
									if(masterIdField != null){
										Predicate<String> masterLabelNameMatcher = TreeNode.makeLabelNameMatcher(masterIdField, definitionName);
										masterNode = masterNode.findByNameField(masterLabelNameMatcher);
									}
									else{
										masterNode = masterNode.findByLabel(labelMatcher);
									}
								}
								
								
								GlobalState.log("Comparing files...");
								masterNode.computeEditMapping(subNode);
								
								Deque<String> masterStrings = masterNode.toStrings();
								Deque<String> subStrings = subNode.toStrings();
								Deque<Integer> masterAnnotations = masterNode.annotations();
								Deque<Integer> subAnnotations = subNode.annotations();
								GlobalState.log(String.format("Done with file '%s' in '%s'.", subFile.getName(), subFile.getMod().getName()));
								return new TreeComparison(masterStrings, subStrings, masterAnnotations, subAnnotations);
							} catch(IOException e){
								e.printStackTrace();
								return null;
							}
						}
					).collect(Collectors.toList())
			);
			
			GlobalState.log("Awaiting");
			for(int i = 0; i < n-1; i++){
				GlobalState.log(String.format("Gathering %d.", i));
				TreeComparison treeComparison = futureList.get(i).get();
				masterStringDeques.add(treeComparison.masterStrings);
				subStringDeques.add(treeComparison.subStrings);
				masterIntegerDeques.add(treeComparison.masterAnnotations);
				subIntegerDeques.add(treeComparison.subAnnotations);
				GlobalState.log(String.format("Got %d.", i));
			}
			
			GlobalState.log("Formatting results.");
			while(!masterIntegerDeques.stream().allMatch(Collection::isEmpty)
					|| !subIntegerDeques.stream().allMatch(Collection::isEmpty)){
				
				List<Integer> nextMasters = masterIntegerDeques.stream().map(Deque::peek).collect(Collectors.toList());
				List<Integer> nextSubs = subIntegerDeques.stream().map(Deque::peek).collect(Collectors.toList());
				
				GlobalState.log(String.format("Peeked %s and %s", String.valueOf(nextMasters), String.valueOf(nextSubs)));
				
				assert IntStream.range(0, n - 1).allMatch(i ->
						nextMasters.get(i) == null || nextMasters.get(i) == 0
								|| nextSubs.get(i) == null || nextSubs.get(i) == 0
								|| nextMasters.get(i).equals(nextSubs.get(i))
				);
				
				String[] entry = getNextRow(n, masterStringDeques, subStringDeques, masterIntegerDeques, subIntegerDeques, nextMasters, nextSubs);
				listList.add(Arrays.asList(entry));
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return listList;
	}
	
	private String[] getNextRow(int n, List<Deque<String>> masterStringDeques, List<Deque<String>> subStringDeques, List<Deque<Integer>> masterIntegerDeques, List<Deque<Integer>> subIntegerDeques, List<Integer> nextMasters, List<Integer> nextSubs){
		String[] entry = new String[n*2];
		Arrays.fill(entry, "");
		for(int i = 0; i < n-1; i++){
			Integer integer = nextSubs.get(i);
			if(integer != null && integer == 0){
				// if there is an insertion, advance only that file
				subIntegerDeques.get(i).pop();
				String insertion = subStringDeques.get(i).pop();
				entry[2*(i+1)] = INSERT;
				entry[2*(i+1)+1] = insertion;
				
				// except if there are identical insertions, then advance those too
				String content = insertion.trim();
				for(int j = i+1; j < n-1; j++){
					Integer integer1 = nextSubs.get(j);
					if(integer1 != null && integer1 == 0){
						String other = subStringDeques.get(j).peek();
						if(other != null && content.equals(other.trim())){
							subIntegerDeques.get(j).pop();
							entry[2*(j+1)] = INSERT;
							entry[2*(j+1)+1] = subStringDeques.get(j).pop();
						}
					}
				}
				return entry;
			}
		}
		// else advance all files that are not removals, including all masters
		for(int i = 0; i < n-1; i++){
			masterIntegerDeques.get(i).pop();
			String insertion = masterStringDeques.get(i).poll();
			entry[0] = "";
			entry[1] = insertion;
		}
		for(int i = 0; i < n-1; i++){
			if(nextMasters.get(i) != 0){
				subIntegerDeques.get(i).pop();
				String keep = subStringDeques.get(i).pop();
				if(keep.trim().equals(entry[1].trim())){
					entry[2*(i+1)] = "";
				}
				else{
					entry[2*(i+1)] = MODIFY;
				}
				entry[2*(i+1)+1] = keep;
			}
			else{
				entry[2*(i+1)] = DELETE;
				entry[2*(i+1)+1] = "";
			}
		}
		return entry;
	}
	
	private void onAlertTableRowClicked(TableRow<Alert> row, MouseEvent event){
		if(event.getClickCount() >= 2 && !row.isEmpty()){
			Alert item = row.getItem();
			Supplier<List<List<String>>> source;
			List<String> names;
			String title;
			switch(item.getKind()){
				case UNDEFINE:{
					Mod.ModFile modFile = item.getFiles().get(0);
					title = String.format("Deleted Definition in \"%s\" in \"%s\".",
							modFile.getName(), modFile.getMod().getName()
					);
					names = List.of("Line");
					source = () -> previewDefinition(modFile, item.getDefinitionName());
					break;
				}
				case PARSE_ERROR:{
					Mod.ModFile modFile = item.getFiles().get(0);
					title = String.format("Attempting to repair \"%s\" in \"%s\".",
							modFile.getName(), modFile.getMod().getName()
					);
					names = List.of("Line", "Original", "Suggestion");
					source = () -> tryRepairFile(modFile);
					break;
				}
				case FILE_CONFLICT:{
					List<Mod.ModFile> modFiles = item.getFiles();
					title = String.format("Comparing instances of \"%s\" in \"%s\".",
							modFiles.get(0).getName(),
							modFiles.stream().map(Mod.ModFile::getMod).map(Mod::getName).collect(Collectors.joining("\", \""))
					);
					names = modFiles.stream().flatMap(modFile -> List.of("State", modFile.getMod().getName()).stream()).collect(Collectors.toList());
					source = () -> compareModFiles(modFiles);
					break;
				}
				case NAME_CONFLICT:{
					String definitionName = item.getDefinitionName();
					List<Mod.ModFile> modFiles = item.getFiles();
					title = String.format("Comparing instances of \"%s\" in \"%s\".",
							definitionName,
							modFiles.stream()
									.map(modFile -> String.format("%s(%s)",modFile.getName(),modFile.getMod().getName()))
									.collect(Collectors.joining("\", \""))
					);
					names = modFiles.stream().flatMap(modFile -> List.of("State", modFile.getName()).stream()).collect(Collectors.toList());
					source = () -> compareModFiles(modFiles, definitionName);
					break;
				}
				default:
					throw new IllegalStateException("Unexpected value: " + item.getKind());
			}
			showComparisonGridStage(primaryStage, title, names, source);
		}
	}
	
	private List<List<String>> previewDefinition(Mod.ModFile modFile, String definitionName){
		final List<List<String>> listList = new ArrayList<>(); // rows x cols
		try{
			final Future<List<String>> listFuture = GlobalState.backgroundExecutor.submit(() -> {
					GlobalState.log(String.format("Parsing file '%s' in '%s'.", modFile.getName(), modFile.getMod().getName()));
					TreeNode node = TreeNode.valueOf(CharStreams.fromPath(modFile.getAbsolutePath(),
							modFile.getGameFolder().getCharset()));
					
					String idField = modFile.getGameFolder().getIdField();
					if(idField != null){
						Predicate<String> labelNameMatcher = TreeNode.makeLabelNameMatcher(idField, definitionName);
						node = node.findByNameField(labelNameMatcher);
					} else {
						Predicate<String> labelMatcher = TreeNode.makeLabelMatcher(definitionName);
						node = node.findByLabel(labelMatcher);
					}
					return node != null ? List.copyOf(node.toStrings()) : List.of();
			});
			List<String> strings = listFuture.get();
			for(String string : strings){
				listList.add(List.of(string));
			}
		} catch(Exception e){
			GlobalState.log(e.getLocalizedMessage());
			e.printStackTrace();
		}
		return listList;
		
	}
	
	private void showComparisonGridStage(Stage primaryStage, String title, List<String> names, Supplier<List<List<String>>> source){
		if(source == null) return; // fail
		int columns = names.size();
		TableView<List<String>> collectionTableView = new TableView<>();
		IntStream.range(0, columns).forEachOrdered(i -> {
			TableColumn<List<String>, String> tableColumn = new TableColumn<>(names.get(i));
			tableColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().get(i)));
			tableColumn.setSortable(false);
			collectionTableView.getColumns().add(tableColumn);
		});
		
		collectionTableView.setRowFactory(param -> new TableRow<>(){
			@Override
			protected void updateItem(List<String> item, boolean empty){
				super.updateItem(item, empty);
				if(item != null){
					if(item.stream().anyMatch(INSERT::equals)){
						setStyle("-fx-background-color:LightBlue");
					}
					else if(item.stream().anyMatch(DELETE::equals)){
						setStyle("-fx-background-color:Coral");
					}
					else if(item.stream().anyMatch(MODIFY::equals)){
						setStyle("-fx-background-color:Khaki");
					}
					else{
						setStyle(null);
					}
				}
			}
		});
		
		Scene scene = new Scene(collectionTableView, 1200, 640);
		Stage comparisonGridStage = new Stage();
		comparisonGridStage.setTitle(title);
		comparisonGridStage.setScene(scene);
		comparisonGridStage.initOwner(primaryStage);
		
		
		Stage progressStage = makeProgressStage(primaryStage);
		progressStage.show();
		GlobalState.backgroundExecutor.submit(() -> {
			try{
				List<List<String>> listList = source.get();
				GlobalState.log("Done.");
				collectionTableView.getItems().setAll(listList);
			}
			catch(Exception e){
				GlobalState.log(e.getLocalizedMessage());
			}
			finally {
				Platform.runLater(() -> {
					progressStage.close();
					comparisonGridStage.show();
				});
			}
		});
	}
	
	private void tryUpdateInstallRoot(){
		if(GlobalState.isSteamFolderOk()){
			GlobalState.log(String.format("%s/steam.exe found.", GlobalState.steamRoot));
			if(GlobalState.game != null){
				Path manifest = GlobalState.game.getManifestFile(GlobalState.steamRoot);
				GlobalState.log(String.format("manifest: %s", manifest));
				if(Files.exists(manifest)){
					GlobalState.log(String.format("Game manifest file '%s' found.", manifest.getFileName()));
					FolderUtils.parseInstallRoot(manifest);
					GlobalState.log(String.format("installRoot: %s", GlobalState.installRoot));
				} else {
					String format = String.format("Error: File '%s' not found.", GlobalState.game.getExecutable());
					gameStateLabel.setText(format);
					GlobalState.log(format);
				}
			}
		} else {
			gameStateLabel.setText("Error: File 'steam.exe' not found.");
		}
	}
	
	private HBox makeFolderNameInput(String name, TextField textField, Consumer<Path> listener){
		Label label = new Label(name);
		label.setAlignment(Pos.BASELINE_CENTER);
		HBox.setHgrow(textField, Priority.ALWAYS);
		DirectoryChooser directoryChooser = new DirectoryChooser();
		Button button = new Button("Browse...");
		
		textField.setOnAction(event -> {
			Path path = Paths.get(textField.getText());
			listener.accept(path);
		});
		button.setOnAction(event -> {
			File file = directoryChooser.showDialog(primaryStage);
			if(file != null && file.canRead()){
				listener.accept(file.toPath());
				textField.setText(file.getPath());
			}
		});
		
		return new HBox(8, label, textField, button);
	}
	
	private void onUpdateDocsFolder(Path path){
		GlobalState.docsFolder = path;
		if(GlobalState.game != null){
			Path settingsFile = GlobalState.game.getSettingsFile(GlobalState.docsFolder);
			if(Files.isReadable(settingsFile)){
				String format = String.format("Success: File '%s' exists.", settingsFile);
				settingsStateLabel.setText(format);
				GlobalState.log(format);
			} else {
				String format = String.format("Error: File '%s' does not exist.", settingsFile);
				settingsStateLabel.setText(format);
				GlobalState.log(format);
			}
			updateModList();
		}
	}
	
	private void onUpdateGamePath(Path path){
		if(path != null){
			GlobalState.installRoot = path.getParent();
			Path gameExePath = GlobalState.game.getGameDataFolder(GlobalState.installRoot).resolve(GlobalState.game.getExecutable());
			if(Files.exists(gameExePath)){
				String format = String.format("Success: File '%s' exists.", gameExePath);
				gameStateLabel.setText(format);
				gamePathField.setText(gameExePath.getParent().toString());
				GlobalState.log(format);
			} else {
				String format = String.format("Error: File '%s' not found.", GlobalState.game.getExecutable());
				gameStateLabel.setText(format);
				GlobalState.log(format);
			}
		}
		updateModList();
	}
	
	
	private void updateModList(){
		if(GlobalState.isDocsFolderOk() && GlobalState.isGameFolderOk() && modTableView != null){
			try{
				modTableView.getItems().clear();
				if(GlobalState.mods != null) GlobalState.mods.close();
				GlobalState.mods = new ModList(GlobalState.game.getSettingsFile(GlobalState.docsFolder));
				modTableView.getItems().setAll(GlobalState.mods.getValues());
				checkConflictsButton.setDisable(false);
			} catch(IOException e){
				GlobalState.log(e.getLocalizedMessage());
				e.printStackTrace();
				checkConflictsButton.setDisable(true);
			}
		}
		else {
			checkConflictsButton.setDisable(true);
		}
	}
	
	private Stage makeProgressStage(Stage parent){
		Stage progressStage = new Stage();
		progressStage.setTitle("This might take a moment...");
		progressStage.setAlwaysOnTop(true);
		progressStage.initOwner(parent);
		progressStage.initModality(Modality.APPLICATION_MODAL);
		progressStage.initStyle(StageStyle.UTILITY);
		
		ProgressBar progressBar = new ProgressBar();
		progressBar.setMaxHeight(40);
		progressBar.setPrefHeight(40);
		progressBar.setMaxWidth(200);
		progressBar.setPrefWidth(200);
		Scene scene = new Scene(new HBox(progressBar), 200, 40);
		progressStage.setScene(scene);
		return progressStage;
	}
	
	private void runConflictCheck(){
		Stage progressStage = makeProgressStage(primaryStage);
		progressStage.show();
		
		GlobalState.backgroundExecutor.submit(() -> {
			GlobalState.alerts.clear();
			List<Alert> alertList = GlobalState.mods.runConflictCheck();
			GlobalState.alerts.addAll(alertList);
			Platform.runLater(() -> {
				alertTableView.getItems().setAll(
						GlobalState.alerts.stream()
								.filter(alert -> alert.getSeverity().compareTo(GlobalState.minimalAlertSeverity) >= 0)
								.collect(Collectors.toList())
				);
				
				progressStage.close();
			});
		});
	}
	
	private TableView<Alert> makeAlertTableView(){
		TableView<Alert> tableView = new TableView<>();
		tableView.setRowFactory(param -> {
			TableRow<Alert> row = new TableRow<>();
			row.setOnMouseClicked((MouseEvent event) ->	onAlertTableRowClicked(row, event));
			return row;
		});
		TableColumn<Alert, String> alertSeverityColumn = new TableColumn<>("Severity");
		alertSeverityColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getSeverity().toString()));
		tableView.getColumns().add(alertSeverityColumn);
		TableColumn<Alert, String> alertKindColumn = new TableColumn<>("Kind");
		alertKindColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getKind().toString()));
		tableView.getColumns().add(alertKindColumn);
		TableColumn<Alert, String> alertMessageColumn = new TableColumn<>("Message");
		alertMessageColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getMessage()));
		alertMessageColumn.setPrefWidth(280);
		tableView.getColumns().add(alertMessageColumn);
		TableColumn<Alert, String> alertFilesColumn = new TableColumn<>("Offending File(s)");
		alertFilesColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().formatFiles()));
		alertFilesColumn.setPrefWidth(480);
		tableView.getColumns().add(alertFilesColumn);
		return tableView;
	}
	
	public void onDebugLog(String s){
		if(debugArea != null){
			if(Platform.isFxApplicationThread()){
				debugArea.appendText(s);
				debugArea.appendText("\n");
			}else {
				Platform.runLater(() -> {
					debugArea.appendText(s);
					debugArea.appendText("\n");
				});
			}
		}
	}
	
	@Override
	public void stop() throws Exception{
		super.stop();
		GlobalState.backgroundExecutor.shutdownNow();
		if(GlobalState.mods != null)
			GlobalState.mods.close();
	}
}
