package org.soundnet.sudunarchiver;

import java.io.File;

import org.pamguard.x3.sud.SudAudioInputStream;
import org.pamguard.x3.sud.SudFileExpander;
import org.pamguard.x3.sud.SudFileMap;
import org.pamguard.x3.sud.SudParams;
import org.soundnet.sudunarchiver.SudUnpackListener.Sud_Message;

import java.util.ArrayList;

import javafx.application.Platform;
import javafx.concurrent.Task;


/**
 * Main controller for unpacking sud files. 
 * @author Jamie Macaulay 
 *
 */
public class SudUnpackerControl {

	/**
	 * The parameters for the sud expander. 
	 */
	private SudUnpackerParams sudExapnderParams = new SudUnpackerParams(); 

	/**
	 * The sud file expander. 
	 */
	private SudFileExpander fileExpander;

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
												
		sudParams.setSudEnable(sudExpanderParams.unPackWav, sudExpanderParams.unPackCSV, sudExpanderParams.unPackClicks);
		
		sudParams.setFileSave(sudExpanderParams.unPackWav, sudExpanderParams.unPackCSV,  
				sudExpanderParams.unPackXML, sudExpanderParams.unPackClicks);
						
		sudParams.setSudFilePath(file.getAbsolutePath());
		
		if (sudParams.saveFolder!=null) {
			sudParams.saveFolder = sudExpanderParams.saveFolder.getAbsolutePath(); 
		}
		else {
			sudParams.saveFolder = null;
		}
		
		sudParams.zeroPad = sudExpanderParams.zeroPad;

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
		
		System.out.println("startNextFileTask: " + currentFileInd);

	
		if (sudExapnderParams.sudFiles == null ||   currentFileInd >= sudExapnderParams.sudFiles.size()) {
			updateListeners(Sud_Message.UNPACK_FINISH, null);
			return;
		}
		
		if (oldTask!=null) {
			this.currentTasks.remove(oldTask); 
		}

		SudFileProcessTask sudProcessTask = new SudFileProcessTask(
				sudExapnderParams.sudFiles.get(currentFileInd), sudExapnderParams); 
		
		updateListeners(Sud_Message.NEW_SUD_FILE, sudProcessTask);

		currentTasks.add(sudProcessTask); 
		
		currentFileInd++; 

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


	public class SudFileProcessTask extends Task<Integer>{

		private SudFileExpander sudFileExpander;

		private File file;

		private int nBlocks;

		private int chunkCount;

		private SudUnpackerParams sudExapnderParams;

		public SudFileProcessTask(File file, SudUnpackerParams sudExapnderParams) {

			this.file = file; 
			this.sudExapnderParams=sudExapnderParams; 
			sudFileExpander = createSudFileExpander( file,  sudExapnderParams);

			sudFileExpander.addSudFileListener(( chunkId,  sudChunk)->{
				//update the progress...
				chunkCount++; 
				
				//System.out.println("Update progress: " + chunkCount + "  " + nBlocks);
				//update the progress of the task. 
				updateProgress(chunkCount, 2*nBlocks);
				
				if (chunkCount%100==0) {
					Platform.runLater(()->{
						updateListeners(Sud_Message.PROGRESS_UPDATE, this); 
					});
				}
			});
		}

		public File getFile() {
			return file;
		}

		@Override
		protected Integer call() throws Exception {

			try {
				
				System.out.println("Creating map: " + file.getAbsolutePath());

				//mape the file
				updateMessage("Mapping SUD file " + file.getName());
				SudFileMap fileMap = SudAudioInputStream.mapSudFile(sudFileExpander, false);
				sudFileExpander.getSudInputStream().close();//make sure the close the input stream so we can reset 
				nBlocks = fileMap.chunkHeaderMap.size(); 
							
				updateProgress(-1, chunkCount);
				System.out.println("SUD file start processing: " + file.getAbsolutePath());

				System.out.println("SUD No. Blocks: " +  nBlocks);
				updateMessage("Processing SUD file" + file.getName());
				sudFileExpander.processFile();
			
				
				System.out.println("SUD file finished processing");
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
			updateMessage("Done!" + file.getName());
			processNextFile();
		}

		@Override protected void cancelled() {
			super.cancelled();
			updateMessage("Cancelled!");
			//processNextFile();
		}

		@Override protected void failed() {
			super.failed();
			updateMessage("Failed!");
			processNextFile();
		}

		public int getFileindex() {
			// TODO a bit dodgy...
			return sudExapnderParams.sudFiles.indexOf(file);
		}
	}

	/**
	 * The current index of the files being processed. 
	 * @return - the current file index. 
	 */
	public int getCurretnFileindex() {
		return currentFileInd;
	}



}
