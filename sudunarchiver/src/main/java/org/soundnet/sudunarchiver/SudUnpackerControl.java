package org.soundnet.sudunarchiver;

import java.io.File;
import java.io.FileNotFoundException;

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

	/**
	 * Snapshot of the parameters used by the current run. This is copied when a run
	 * starts so that selecting a new folder (or save location) in the UI cannot change
	 * the list of files a run is working through.
	 */
	private SudUnpackerParams runParams;

	/**
	 * Identifies the current run. Tasks from an older run (e.g. one that was stopped)
	 * never pick up files from, or send updates for, a newer run.
	 */
	private volatile int runId = 0;

	/**
	 * The number of tasks in the current run that have not yet finished.
	 */
	private int activeTasks = 0;


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
												
		sudParams.setSudEnable(sudExpanderParams.unPackWav, sudExpanderParams.unPackCSV, sudExpanderParams.unPackClicks, sudExpanderParams.unPackMag);
		
		sudParams.setFileSave(sudExpanderParams.unPackWav, sudExpanderParams.unPackCSV,  
				sudExpanderParams.unPackXML, sudExpanderParams.unPackClicks, sudExpanderParams.unPackMag);
						
		sudParams.setSudFilePath(file.getAbsolutePath());
		
		if (sudExpanderParams.saveFolder!=null) {
			sudParams.saveFolder = sudExpanderParams.saveFolder.getAbsolutePath(); 
		}
		else {
			sudParams.saveFolder = null;
		}
		
		System.out.println("SAVE FOLDER: " + sudParams.saveFolder ); 
		
		sudParams.zeroPad = sudExpanderParams.zeroPad;
		
		
		System.out.println("SAVE PARAMS: " + sudParams); 


		fileExpander.setSudParams(sudParams);

		return fileExpander; 
	}

	/**
	 * Process all sud files in the list. This splits the sud files between different
	 * threads if there are any. 
	 */
	public synchronized void processSudFiles() {
		runId++;
		runParams = sudExapnderParams.copy();
		if (runParams.sudFiles == null) runParams.sudFiles = new ArrayList<File>();

		currentTasks.clear();
		activeTasks = 0;
		currentFileInd = 0;

		updateListeners(Sud_Message.UNPACK_START, null);

		int nThreads = Math.max(1, runParams.nThreads);
		for (int i=0; i<nThreads; i++) {
			startNextFileTask();
		}

		//no files to process at all.
		if (activeTasks==0) {
			updateListeners(Sud_Message.UNPACK_FINISH, null);
		}
	}

	/**
	 * Start the next file task for the current run, if there are any files left.
	 * @return true if a new task was started.
	 */
	private synchronized boolean startNextFileTask() {

		if (currentFileInd >= runParams.sudFiles.size()) {
			return false;
		}

		SudFileProcessTask sudProcessTask = new SudFileProcessTask(
				runParams.sudFiles.get(currentFileInd), runParams, runId, currentFileInd);

		updateListeners(Sud_Message.NEW_SUD_FILE, sudProcessTask);

		currentTasks.add(sudProcessTask);
		activeTasks++;

		currentFileInd++;

		Thread th = new Thread(sudProcessTask);
		th.setDaemon(true);
		th.start();

		return true;
	}

	/**
	 * Called when a task has finished (successfully or not). Starts the next file in
	 * the same run and only reports that the run has finished once every task in it
	 * has completed.
	 * @param task - the task that has finished.
	 */
	private synchronized void taskFinished(SudFileProcessTask task) {
		currentTasks.remove(task);

		//a task from an old run - it must not start processing files from the current run.
		if (task.runId != runId) return;

		activeTasks--;
		startNextFileTask();

		if (activeTasks==0) {
			updateListeners(Sud_Message.UNPACK_FINISH, null);
		}
	}

	/**
	 * Send an update for a task, but only if the task belongs to the current run.
	 * @param message - the message.
	 * @param task - the task.
	 */
	private void updateTaskListeners(Sud_Message message, SudFileProcessTask task) {
		if (task.runId == runId) {
			updateListeners(message, task);
		}
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

	/**
	 * Get the parameters for the run currently in progress (or the last run).
	 * @return the parameters for the current run - can be null if nothing has been run.
	 */
	public SudUnpackerParams getRunParams() {
		return runParams;
	}

	public synchronized void stop() {
		//invalidate the current run so nothing more is started from it.
		runId++;
		for (SudFileProcessTask task : new ArrayList<SudFileProcessTask>(currentTasks)) {
			task.cancel(true);
		}
		currentTasks.clear();
		activeTasks = 0;
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

		/**
		 * The run this task belongs to.
		 */
		private final int runId;

		/**
		 * The index of the file within the run's file list.
		 */
		private final int fileIndex;

		public SudFileProcessTask(File file, SudUnpackerParams sudExapnderParams, int runId, int fileIndex) {

			this.file = file;
			this.runId = runId;
			this.fileIndex = fileIndex;
			sudFileExpander = createSudFileExpander( file,  sudExapnderParams);

			sudFileExpander.addSudFileListener(( chunkId,  sudChunk)->{
				//update the progress...
				chunkCount++; 
				
				//System.out.println("Update progress: " + chunkCount + "  " + nBlocks);
				//update the progress of the task. 
				updateProgress(chunkCount, 2*nBlocks);
				
				if (chunkCount%100==0) {
					Platform.runLater(()->{
						updateTaskListeners(Sud_Message.PROGRESS_UPDATE, this);
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
				

				
				//map the file
				updateMessage("Opening SUD file " + file.getName());
				
				//does a map exist already?
				File sudMapFile = new File(sudFileExpander.getSudFile().getAbsolutePath()+"x"); 
				
				//can we write to the location?
				
				
				boolean exists = sudMapFile.exists(); 
				
				SudFileMap fileMap;
				if (!exists) {
					
					System.out.println("Creating map: " + file.getAbsolutePath());

					fileMap = mapSudFile( sudMapFile);
					
					//must be after the close so that if the write fails we still close extracted files. 
					SudAudioInputStream.saveSudMap(fileMap, sudMapFile);

				}
				else {
					fileMap = SudAudioInputStream.loadSudMap(sudMapFile); 
					if (fileMap==null) {
						//old sud map fiel that cannot be opened
						fileMap = mapSudFile( sudMapFile);
						SudAudioInputStream.saveSudMap(fileMap, sudMapFile);
					}
				}
				
				//we only want the number of blocks with chunk Ids that we actually be using//////TODO
				
				nBlocks = fileMap.chunkHeaderMap.size(); 
				
							
				updateProgress(-1, chunkCount);
				System.out.println("SUD file start processing: " + file.getAbsolutePath());

				System.out.println("SUD No. Blocks: " +  nBlocks);
				updateMessage("Processing SUD file" + file.getName());
				sudFileExpander.processFile();
			
				
				System.out.println("SUD file finished processing");
			}
			catch (FileNotFoundException F) {
				System.err.println("The sud file map could not be written - likely read only drive");
				
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			return 0;
		}

		/**
		 * Called on the FX thread once the task has completed.
		 */
		private void processNextFile() {
			updateTaskListeners(Sud_Message.END_SUD_FILE, this);
			taskFinished(this);
		}

		@Override protected void succeeded() {
			super.succeeded();
			updateProgress(chunkCount, chunkCount);

			updateMessage("Done! " + file.getName());
			processNextFile();
		}

		@Override protected void cancelled() {
			super.cancelled();
			updateMessage("Cancelled! " + file.getName());
			//processNextFile();
		}

		@Override protected void failed() {
			super.failed();
			updateMessage("Failed! " + file.getName());
			processNextFile();
		}
		
		private SudFileMap mapSudFile(File sudMapFile) throws Exception {

			updateProgress(-1, chunkCount);
			//mape the file
			updateMessage("Mapping SUD file " + file.getName());
			
			SudFileMap fileMap = SudAudioInputStream.mapSudFile(sudFileExpander, null, true);
			
//			System.out.println("----Total sud file samples: !!! " + fileMap.getTotalSamples());
		
			sudFileExpander.getSudInputStream().close();//make sure the close the input stream so we can reset 
			
			return fileMap;
		}

		public int getFileindex() {
			return fileIndex;
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
