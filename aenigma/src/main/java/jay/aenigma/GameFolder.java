package jay.aenigma;

import jay.aenigma.ckii.NamingType;

import java.nio.charset.Charset;
import java.util.regex.Pattern;

/**
 * Interface that represents the properties of {@link jay.aenigma.Mod.ModFile}s found in a particular folder of a {@link Game}.
 *
 * TODO: Is this really a suitable abstraction of the concept at hand? Consider alternatives.
 */
public interface GameFolder{
	
	/**Retrieves the {@link String} representation of the relative {@link java.nio.file.Path} of this GameFolder
	 * @return String representation of the relative Path
	 */
	String getPath();
	
	/**Retrieve a {@link Pattern} that matches reserved names, i.e. Names that {@link Definition}s belonging to this
	 * GameFolder may not have.
	 * @return Pattern that matches reserved names.
	 */
	Pattern getReservedNames();
	
	/**Retrieves the {@link MergeBehaviour} that {@link Definition}s belonging to this GameFolder exhibit.
	 * @return MergeBehaviour of Definitions of this GameFolder
	 */
	MergeBehaviour getMergeBehaviour();
	
	/**Retrieves a {@link Pattern} that checks if a {@link java.io.File} Name is valid for this GameFolder.
	 * @return Pattern to match File Names against.
	 */
	Pattern getFileNameRegex();
	
	/**Retrieves the {@link NamingType} that {@link Definition}s belonging to this GameFolder use.
	 * @return NamingType for Definitions of this GameFolder
	 */
	NamingType getNamingType();
	
	/**Iff this GameFolder uses a {@link NamingType} with ID field, such as {@link NamingType#ANY_LEVEL_ID_FIELD},
	 * then this retrieves the name of said ID field.
	 * @return name of the ID field
	 */
	String getIdField();
	
	/**Retrieves the {@link Charset} that {@link java.io.File}s belonging to this GameFolder are encoded with.
	 * @return Charset for Files of this GameFolder
	 */
	Charset getCharset();
}
