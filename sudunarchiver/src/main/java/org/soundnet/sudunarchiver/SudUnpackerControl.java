package org.soundnet.sudunarchiver;

import java.io.File;

import org.pamguard.x3.sud.SudFileExpander;
import org.pamguard.x3.sud.SudHeader;
import org.pamguard.x3.sud.SudParams;
import org.soundnet.sudunarchiver.SudUnpackListener.Sud_Message;

import java.util.ArrayList;

import javafx.concurrent.Task;


/**
 * Main controller for unpacking sud files. 
 * @author Jamie Macaulay 
 *
 */
public class SudUnpackerControl {

	SudUnpackerParams sudExapnderParams = new SudUnpackerParams(); 

	SudFileExpander fileExpander;

	/**
	 * The current file index. 
	 */
	private volatile int currentFileInd; 

	/**
	 * Listeners for updates to SUD file processing. 
	 */
	public ArrayList<SudUnpackListener> listeners = new ArrayList<SudUnpackListener>(); 


	/**
	 * The currently running tasks. 
	 */
	public ArrayList<SudFileProcessTask> currentTasks = new ArrayList<SudFileProcessTask>(); 


	public SudUnpackerControl() {

	}


	/**
	 * Create and set up a file to expand for a sud file. 
	 * @param file - the file to expand. 
	 * @param sudExpanderParams - the parameters. 
	 * @return the file expander for a file. 
	 */
	private SudFileExpander createSudFileExpander(File file, SudUnpackerParams sudExpanderParams) {

		SudFileExpander fileExpander = new SudFileExpander(file); 

		SudParams sudParams = new SudParams(); 
		sudParams.setFileSave(sudExpanderParams.unPackWav, sudExpanderParams.unPackCSV,  
				sudExpanderParams.unPackXML, sudExpanderParams.unPackClicks);
		sudParams.saveFolder = sudExpanderParams.saveFolder.getAbsolutePath(); 

		fileExpander.setSudParams(sudParams);

		return fileExpander; 
	}

	/**
	 * Process all sud files in the list. This splits the sud files between different
	 * threads if there are any. 
	 */
	public void processSudFiles() {
		currentFileInd = 0; 
		for (int i=0; i<sudExapnderParams.nThreads; i++) {
			startNextFileTask(null); 
		}

	}

	/**
	 * Start the next file task. 
	 */
	private synchronized void startNextFileTask(SudFileProcessTask oldTask) {

		currentFileInd++; 

		if (oldTask!=null) {
			this.currentTasks.remove(oldTask); 
		}

		SudFileProcessTask sudProcessTask = new SudFileProcessTask(sudExapnderParams.sudFiles.get(currentFileInd), sudExapnderParams); 

		currentTasks.add(sudProcessTask); 

		Thread th = new Thread(sudProcessTask);
		th.setDaemon(true);
		th.start();
	}


	/**
	 * Get the sud parameters. 
	 * @return the sud paramters. . 
	 */
	public SudUnpackerParams getSudParams() {
		return sudExapnderParams;
	}


	public void setSudParams(SudUnpackerParams sudParams) {
		this.sudExapnderParams = sudParams;
	}

	public void stop() {
		for (int i=0; i<this.currentTasks.size(); i++) {
			currentTasks.get(i).cancel(true); 
		}
	}

	/**
	 * Add a sud listener that receives updates from the control
	 * @param aListener - the listener to add . 
	 */
	public void addSudListener(SudUnpackListener aListener) {
		listeners.add(aListener); 
	}

	/**
	 * Remove a sud listener. 
	 * @param aListener - the listener to remove.
	 * @return true if the listener was in the list and removed. 
	 */
	public boolean removeSudListener(SudUnpackListener aListener) {
		return 	listeners.remove(aListener); 
	}

	/**
	 * Update all listeners. 
	 */
	public void updateListeners(Sud_Message message, Object data) {
		for (SudUnpackListener alIstener: listeners) {
			alIstener.sudUnpackUpdate(message, data);
		}
	}



	class SudFileProcessTask extends Task<Integer>{

		private SudFileExpander sudFileExpander;

		private File file;

		private int nBlocks;

		private int chunkCount;

		public SudFileProcessTask(File file, SudUnpackerParams sudExapnderParams) {

			this.file = file; 
			sudFileExpander = createSudFileExpander( file,  sudExapnderParams);

			sudFileExpander.addSudFileListener(( chunkId,  sudChunk)->{
				//update the progress...
				chunkCount++; 
				//update the progress of the task. 
				updateProgress(nBlocks, chunkCount);
			});
		}

		@Override
		protected Integer call() throws Exception {

			try {
				//take a peek at the file to get the number of chunks
				SudHeader header = sudFileExpander.openSudFile( sudFileExpander.getSudFile()); 
				sudFileExpander.getSudInputStream().close();//make sure the close the input stream so we can reset 

				nBlocks = header.NoOfBlocks; 

				sudFileExpander.processFile();

			}
			catch (Exception e) {
				e.printStackTrace();
			}

			return 0;
		}

		private void processNextFile() {
			startNextFileTask(this); 
		}

		@Override protected void succeeded() {
			super.succeeded();
			updateMessage("Done!");
			processNextFile();
		}

		@Override protected void cancelled() {
			super.cancelled();
			updateMessage("Cancelled!");
			processNextFile();
		}

		@Override protected void failed() {
			super.failed();
			updateMessage("Failed!");
			processNextFile();
		}



	}



}
