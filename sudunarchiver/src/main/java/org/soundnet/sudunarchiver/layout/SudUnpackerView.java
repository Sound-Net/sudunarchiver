package org.soundnet.sudunarchiver.layout;

import org.soundnet.sudunarchiver.SudUnpackListener.Sud_Message;

import org.soundnet.sudunarchiver.SudUnpackerControl;
import org.soundnet.sudunarchiver.SudUnpackerControl.SudFileProcessTask;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.Window;
import jfxtras.styles.jmetro.JMetroStyleClass;
import jfxtras.styles.jmetro.Style;
import jfxtras.styles.jmetro.JMetro;

/**
 * The layout and controls for SUD unpacking 
 * @author Jamie Macaulay
 *
 */
public class SudUnpackerView extends BorderPane {

	private SudUnpackerPane sudUnpackerPane;

	private Stage stage;

	private SudUnpackerControl sudUnpackerControl;  
	
	/**
	 * The progress for each file. 
	 */
	private double[] progress; 

	public SudUnpackerView(SudUnpackerControl sudUnpackerControl, Stage stage) {

		this.sudUnpackerControl = sudUnpackerControl; 
		sudUnpackerControl.addSudListener((message, data)->{
			notifyUpdate( message,  data);  
		});

		this.stage=stage; 

		sudUnpackerPane = new SudUnpackerPane(this); 

		this.setCenter(sudUnpackerPane);
	}

	public SudUnpackerPane getMainPane() {
		return sudUnpackerPane; 
	}



	public static void setTheme(Scene scene, Pane root) {
		JMetro jMetro = new JMetro(Style.LIGHT);
		jMetro.setScene(scene);
		root.getStyleClass().add(JMetroStyleClass.BACKGROUND);
	}

	public Window getStage() {
		return stage;
	}

	public void run() {
	
		
		//set the parameters. 
		sudUnpackerPane.getParams(sudUnpackerControl.getSudParams());
		
		System.out.println("Hello SUD files: " + sudUnpackerControl.getSudParams().sudFiles.size());
		
		progress= new double[sudUnpackerControl.getSudParams().sudFiles.size()]; 
		
		//process the sud files. A listener will update the tasks 
		sudUnpackerControl.processSudFiles();
	}

	public void stop() {
		sudUnpackerControl.stop();
	}

	/**
	 * Called whenever there is an update from the control. 
	 */
	public void notifyUpdate(Sud_Message message, Object data) {
		switch (message) {
		case NEW_SUD_FILE:
			sudUnpackerPane.getTaskView().getTasks().add(0, (SudFileProcessTask) data); 
			sudUnpackerPane.getProgressBar().setProgress(((double) sudUnpackerControl.getCurretnFileindex())/sudUnpackerControl.getSudParams().sudFiles.size());

			break;
		case END_SUD_FILE:
			//just incase. 
			sudUnpackerPane.getTaskView().getTasks().remove((SudFileProcessTask) data); 
			
			this.progress[((SudFileProcessTask) data).getFileindex()]=1.; 

			break;
		case UNPACK_FINISH:
			break;
		case UNPACK_START:
			sudUnpackerPane.getTaskView().getTasks().clear();
			sudUnpackerPane.getProgressBar().setProgress(0);
		case PROGRESS_UPDATE:
			this.progress[((SudFileProcessTask) data).getFileindex()]=((SudFileProcessTask) data).getProgress(); 
			
			
			sudUnpackerPane.getProgressBar().setProgress(getOverallProgress());
			

			break;
		default:
			break;
		
		}
	}
	
	/**
	 * Get the overall progress.
	 * @return the overall progress.
	 */
	private double getOverallProgress() {
		double progress=0; 
		for (int i=0; i<this.progress.length; i++) {
			progress+=this.progress[i];
		}
		return progress/this.progress.length; 
	}
	


}
