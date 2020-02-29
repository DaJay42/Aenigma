package jay.aenigma;

import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of {@link org.antlr.v4.runtime.ANTLRErrorListener} that turns encountered errors into
 * {@link Alert}s. Upon completed parsing, the {@link List} of encountered Alerts can be retrieved with
 * {@link AlertErrorListener#getAlerts()}.
 */
class AlertErrorListener extends ConsoleErrorListener{
	private final Mod.ModFile modFile;
	private final List<Alert> alerts;
	
	/**Creates a new instance whose {@link Alert}s will refer to the given {@link jay.aenigma.Mod.ModFile}
	 * @param modFile the ModFile that Alerts shall refer to
	 */
	AlertErrorListener(Mod.ModFile modFile){
		this.modFile = modFile;
		this.alerts = new ArrayList<>();
	}
	
	/**Retrieves an <a href="Collection.html#unmodview">unmodifiable view</a> of the accumulated {@link List} of {@link Alert}s.
	 * @return unmodifiable view of list of encountered alerts.
	 */
	public List<Alert> getAlerts(){
		return Collections.unmodifiableList(alerts);
	}
	
	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
							int line, int charPositionInLine, String msg, RecognitionException e){
		alerts.add(new Alert(Severity.ERROR, Alert.Kind.PARSE_ERROR, modFile.getGameFolder(), List.of(modFile),
				null, String.format("Failure at line %d:%d due to:\n %s", line, charPositionInLine, msg)));
	}
}
