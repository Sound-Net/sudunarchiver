package org.soundnet.sudunarchiver;

import java.io.File;
import java.util.ArrayList;

/**
 * The parameters for the sud unpacker. 
 * @author Jamie Macaulay 
 *
 */
public class SudUnpackerParams {
	
	/**
	 * List of folders or files to unpack. 
	 */
	ArrayList<File> unpackFiles = null;
	
	/**
	 * List of folders or files to unpack. 
	 */
	File saveFolder = null;
	
	/**
	 * True for sub folders to be used. 
	 */
	boolean subFolder = true; 
	
	/**
	 * True to unpack raw wav files
	 */
	boolean unPackWav = true; 
	
	/**
	 * True to unpack the xml files 
	 */
	boolean unPackXML = true; 

	/**
	 * True to unpack CSV files
	 */
	boolean unPackCSV = true; 

	/**
	 * True to unpack clicks. 
	 */
	boolean unPackClicks = true; 


}
