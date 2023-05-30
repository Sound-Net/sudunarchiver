package org.soundnet.sudunarchiver;

import java.io.File;

import org.pamguard.x3.sud.SudFileExpander;
import org.pamguard.x3.sud.SudParams;


/**
 * Main controller for unpacking sud files. 
 * @author Jamie Macaulay 
 *
 */
public class SudUnpackerControl {
	
	SudUnpackerParams sudParams = new SudUnpackerParams(); 
	
	SudFileExpander fileExpander; 
	

	public SudUnpackerControl() {
		
	}
	
	private SudFileExpander createSudFileExpander(File file, SudUnpackerParams sudParams) {
		
		SudFileExpander fileExpander = new SudFileExpander(file); 
		
		SudParams sudExpanderParams = new SudParams(); 
		
		sudExpanderParams.saveWav = sudParams.unPackWav; 
		sudExpanderParams.saveMeta = sudParams.unPackXML;


		fileExpander.setSudParams(null);
		
	}
	
	public SudUnpackerParams getSudParams() {
		return sudUnpackParams;
	}


	public void setSudParams(SudUnpackerParams sudParams) {
		this.sudParams = sudParams;
	}

	public void stop() {
		// TODO Auto-generated method stub
		
	}



}
