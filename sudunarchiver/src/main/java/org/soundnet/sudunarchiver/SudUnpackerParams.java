package org.soundnet.sudunarchiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The parameters for the sud unpacker. 
 * @author Jamie Macaulay 
 *
 */
public class SudUnpackerParams {
	
	/**
	 * List of folders or files to unpack. 
	 */
	public List<File> sudFiles = null;
	
	/**
	 * List of folders or files to unpack. 
	 */
	public File saveFolder = null;
	
	/**
	 * True for sub folders to be used. 
	 */
	public boolean subFolder = true; 
	
	/**
	 * True to unpack raw wav files
	 */
	public boolean unPackWav = true; 
	
	/**
	 * True to unpack the xml files 
	 */
	public boolean unPackXML = true; 

	/**
	 * True to unpack CSV files
	 */
	public boolean unPackCSV = true; 

	/**
	 * True to unpack clicks. 
	 */
	public boolean unPackClicks = true; 
	
	/**
	 * The number of threads to process on....be careful not to mely your computer with 
	 * this one. 
	 */
	public int nThreads = 1;

	/**
	 * True to zero pad the wav files
	 */
	public boolean zeroPad;

	/**
	 * True to unpack the mag files
	 */
	public boolean unPackMag = true;

	/**
	 * Create an independent copy of the parameters, including a copy of the file
	 * list, so that changes made in the UI do not affect a run that is in progress.
	 * @return a copy of the parameters.
	 */
	public SudUnpackerParams copy() {
		SudUnpackerParams params = new SudUnpackerParams();
		params.sudFiles = sudFiles == null ? null : new ArrayList<File>(sudFiles);
		params.saveFolder = saveFolder;
		params.subFolder = subFolder;
		params.unPackWav = unPackWav;
		params.unPackXML = unPackXML;
		params.unPackCSV = unPackCSV;
		params.unPackClicks = unPackClicks;
		params.nThreads = nThreads;
		params.zeroPad = zeroPad;
		params.unPackMag = unPackMag;
		return params;
	}

}
