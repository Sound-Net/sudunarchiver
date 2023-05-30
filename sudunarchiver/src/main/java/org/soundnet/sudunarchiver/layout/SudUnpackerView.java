package org.soundnet.sudunarchiver.layout;

import org.soundnet.sudunarchiver.SudUnpackerControl;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
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

	public SudUnpackerView(SudUnpackerControl sudUnpackerControl, Stage stage) {
	
		
		sudUnpackerPane = new SudUnpackerPane(stage); 
		
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
	



}
