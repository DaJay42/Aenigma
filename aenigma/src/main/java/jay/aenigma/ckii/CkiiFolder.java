package jay.aenigma.ckii;

import jay.aenigma.FolderUtils;
import jay.aenigma.GameFolder;
import jay.aenigma.MergeBehaviour;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

public enum CkiiFolder implements GameFolder{
	//TODO: resolve all these UNKNOWNs...
	
	// in /common/
	ALTERNATE_START(MergeBehaviour.UNKNOWN),
	ARTIFACT_SPAWNS(MergeBehaviour.REPLACE),
	ARTIFACTS(MergeBehaviour.REPLACE),
	BLOODLINES(MergeBehaviour.UNKNOWN),
	BOOKMARKS(MergeBehaviour.REPLACE),
	BUILDINGS(MergeBehaviour.UNKNOWN, NamingType.SECOND_LEVEL),
	CB_TYPES(MergeBehaviour.FAIL_ON_DUPLICATE),
	COMBAT_TACTICS(MergeBehaviour.UNKNOWN),
	COUNCIL_POSITIONS(MergeBehaviour.UNKNOWN),
	COUNCIL_VOTING(MergeBehaviour.MERGE_GROUPS),
	CULTURES(MergeBehaviour.MERGE_GROUPS, NamingType.SECOND_LEVEL),
	DEATH(MergeBehaviour.UNKNOWN),
	DEATH_TEXT(MergeBehaviour.UNKNOWN),
	DEFINES(MergeBehaviour.REPLACE),
	DISEASE(MergeBehaviour.NOT_APPLICABLE),
	DYNASTIES(MergeBehaviour.PARTIAL_COMPLEX_MAYBE),
	EVENT_MODIFIERS(MergeBehaviour.REPLACE),
	EXECUTION_METHODS(MergeBehaviour.UNKNOWN),
	GAME_RULES(MergeBehaviour.REPLACE),
	GOVERNMENT_FLAVOR(MergeBehaviour.UNKNOWN, NamingType.ID_FIELD),
	GOVERNMENTS(MergeBehaviour.MERGE_GROUPS),
	GRAPHICALCULTURETYPES(MergeBehaviour.UNKNOWN),
	HEIR_TEXT(MergeBehaviour.UNKNOWN),
	HOLDING_TYPES(MergeBehaviour.NONE),
	JOB_ACTIONS(MergeBehaviour.REPLACE),
	JOB_TITLES(MergeBehaviour.UNKNOWN),
	LANDED_TITLES(MergeBehaviour.PARTIAL_COMPLEX, NamingType.PREFIX_ANY_LEVEL),
	LAWS(MergeBehaviour.FAIL_ON_DUPLICATE, NamingType.SECOND_LEVEL),
	MERCENARIES(MergeBehaviour.UNKNOWN),
	MINOR_TITLES(MergeBehaviour.FAIL_ON_DUPLICATE),
	MODIFIER_DEFINITIONS(MergeBehaviour.UNKNOWN),
	NICKNAMES(MergeBehaviour.REPLACE),
	OBJECTIVES(MergeBehaviour.UNKNOWN),
	OFFMAP_POWERS(MergeBehaviour.UNKNOWN),
	POLICIES("common/offmap_powers/", MergeBehaviour.UNKNOWN),
	STATUSES("common/offmap_powers/", MergeBehaviour.UNKNOWN),
	ON_ACTIONS(MergeBehaviour.MERGE_GROUPS),
	OPINION_MODIFIERS(MergeBehaviour.UNKNOWN),
	RELIGION_FEATURES(MergeBehaviour.FAIL_ON_DUPLICATE, NamingType.SECOND_LEVEL),
	RELIGION_MODIFIERS(MergeBehaviour.UNKNOWN),
	RELIGIONS(MergeBehaviour.MERGE_GROUPS, NamingType.SECOND_LEVEL),
	RELIGIOUS_TITLES(MergeBehaviour.UNKNOWN),
	RETINUE_SUBUNITS(MergeBehaviour.FAIL_ON_DUPLICATE),
	SAVE_CONVERSION(MergeBehaviour.NOT_APPLICABLE),
	SCRIPTED_EFFECTS(MergeBehaviour.UNKNOWN),
	SCRIPTED_SCORE_VALUES(MergeBehaviour.UNKNOWN),
	SCRIPTED_TRIGGERS(MergeBehaviour.FAIL_ON_DUPLICATE),
	SOCIETIES(MergeBehaviour.FAIL_ON_DUPLICATE),
	SPECIAL_TROOPS(MergeBehaviour.UNKNOWN),
	SUCCESSION_VOTING(MergeBehaviour.UNKNOWN),
	TRADE_ROUTES(MergeBehaviour.UNKNOWN),
	TRAITS(MergeBehaviour.FAIL_ON_DUPLICATE),
	TRIBUTARY_TYPES(MergeBehaviour.UNKNOWN),
	TRIGGERED_MODIFIERS(MergeBehaviour.UNKNOWN),
	
	// the rest
	DECISIONS("", MergeBehaviour.FAIL_ON_DUPLICATE, NamingType.SECOND_LEVEL),
	EU4_CONVERTER("", MergeBehaviour.UNKNOWN),
	EVENTS("", MergeBehaviour.UNSAFE, NamingType.ID_FIELD),
	
	
	// History files
	CHARACTERS("history/", MergeBehaviour.FAIL_ON_DUPLICATE),
	HISTORY_OFFMAP_POWERS("history/", MergeBehaviour.NOT_APPLICABLE),
	PROVINCES("history/", MergeBehaviour.NOT_APPLICABLE),
	TECHNOLOGY("history/", MergeBehaviour.NOT_APPLICABLE),
	TITLES("history/", MergeBehaviour.NOT_APPLICABLE),
	WARS("history/", MergeBehaviour.NOT_APPLICABLE, NamingType.ANY_LEVEL_ID_FIELD),
	
	//interface
	INTERFACE("", MergeBehaviour.MERGE_GROUPS, FolderUtils.gfxPattern, NamingType.SECOND_LEVEL_ID_FIELD),
	COAT_OF_ARMS("interface/", MergeBehaviour.NOT_APPLICABLE),
	PORTRAIT_OFFSETS("interface/", MergeBehaviour.NOT_APPLICABLE, FolderUtils.csvPattern),
	PORTRAIT_PROPERTIES("interface/", MergeBehaviour.NOT_APPLICABLE),
	PORTRAITS("interface/", MergeBehaviour.MERGE_GROUPS, FolderUtils.gfxPattern, NamingType.SECOND_LEVEL_ID_FIELD),
	
	LAUNCHER_INTERFACE("launcher/", MergeBehaviour.NOT_APPLICABLE, FolderUtils.gfxPattern, NamingType.ANY_LEVEL_ID_FIELD),
	
	LOCALISATION("", MergeBehaviour.IGNORED, FolderUtils.csvPattern),
	CUSTOMIZABLE_LOCALISATION("localisation/", MergeBehaviour.UNKNOWN, NamingType.ID_FIELD),
	
	//MAP("", MergeBehaviour.UNKNOWN),
	//STATICS("/map", MergeBehaviour.UNKNOWN),
	//TERRAIN("/map", MergeBehaviour.UNKNOWN),
	
	//MOD("", MergeBehaviour.NONE),
	
	//MUSIC("", MergeBehaviour.UNKNOWN),
	//SOUND("", MergeBehaviour.UNKNOWN),
	//TUTORIAL("", MergeBehaviour.UNKNOWN),
	
	;
	
	String pathPrefix = "common/";
	MergeBehaviour mergeBehaviour;
	
	Pattern regex = FolderUtils.txtPattern;
	NamingType namingType = NamingType.TOP_LEVEL;
	
	
	
	public String getPath(){
		switch(this){
			case HISTORY_OFFMAP_POWERS:
				return pathPrefix + OFFMAP_POWERS.name().toLowerCase();
			case LAUNCHER_INTERFACE:
				return pathPrefix + INTERFACE.name().toLowerCase();
			default:
				return pathPrefix + this.name().toLowerCase();
		}
	}
	
	@Override
	public Pattern getReservedNames(){
		switch(this){
			case ARTIFACTS:
				return Pattern.compile("^$|^slots$");
			case CULTURES:
				return Pattern.compile("^$|^alternate_start$|^graphical_cultures$");
			case RELIGIONS:
				return Pattern.compile("^$|^color$|^male_names$|^female_names$|^secret_religion_visibility_trigger$");
			case RELIGION_FEATURES:
				return Pattern.compile("^$|^buttons$");
			default:
				return null;
		}
	}
	
	public MergeBehaviour getMergeBehaviour(){
		return mergeBehaviour;
	}
	
	public Pattern getFileNameRegex(){
		return regex;
	}
	
	public NamingType getNamingType(){
		return namingType;
	}
	
	@Override
	public String getIdField(){
		switch(this){
			case EVENTS:
				return "id";
			case CUSTOMIZABLE_LOCALISATION:
			case GOVERNMENT_FLAVOR:
			case PORTRAITS:
			case LAUNCHER_INTERFACE:
			case INTERFACE:
			case WARS:
				return "name";
			default:
				return null;
		}
	}
	
	@Override
	public Charset getCharset(){
		return Charset.forName("windows-1252");
	}
	
	/// Constructors (all relevant combinations)
	
	CkiiFolder(String pathPrefix, MergeBehaviour mergeBehaviour, Pattern regex){
		this.pathPrefix = pathPrefix;
		this.mergeBehaviour = mergeBehaviour;
		this.regex = regex;
	}
	
	
	CkiiFolder(MergeBehaviour mergeBehaviour){
		this.mergeBehaviour = mergeBehaviour;
	}
	
	CkiiFolder(String pathPrefix, MergeBehaviour mergeBehaviour){
		this.pathPrefix = pathPrefix;
		this.mergeBehaviour = mergeBehaviour;
	}
	
	CkiiFolder(MergeBehaviour mergeBehaviour, Pattern regex){
		this.mergeBehaviour = mergeBehaviour;
		this.regex = regex;
	}
	
	CkiiFolder(MergeBehaviour mergeBehaviour, NamingType namingType){
		this.mergeBehaviour = mergeBehaviour;
		this.namingType = namingType;
	}
	
	CkiiFolder(String pathPrefix, MergeBehaviour mergeBehaviour, Pattern regex, NamingType namingType){
		this.pathPrefix = pathPrefix;
		this.mergeBehaviour = mergeBehaviour;
		this.regex = regex;
		this.namingType = namingType;
	}
	
	CkiiFolder(MergeBehaviour mergeBehaviour, Pattern regex, NamingType namingType){
		this.mergeBehaviour = mergeBehaviour;
		this.regex = regex;
		this.namingType = namingType;
	}
	
	CkiiFolder(String pathPrefix, MergeBehaviour mergeBehaviour, NamingType namingType){
		this.pathPrefix = pathPrefix;
		this.mergeBehaviour = mergeBehaviour;
		this.namingType = namingType;
	}
}
