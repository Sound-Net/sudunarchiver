package org.soundnet.sudunarchiver;

import org.soundnet.sudunarchiver.layout.SudUnpackerView;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


/**
 * JavaFX App
 */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        var javaVersion = SystemInfo.javaVersion();
        var javafxVersion = SystemInfo.javafxVersion();

        System.out.println("JavaFX " + javafxVersion + ", running on Java " + javaVersion + ".");
        
        var sudcontrol = new SudUnpackerControl(); 
        
        var view = new SudUnpackerView(sudcontrol, stage); 
        
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
            	sudcontrol.stop(); 
                Platform.exit();
                System.exit(0);
            }
        });
        
        var scene = new Scene(new StackPane(view.getMainPane()), 500, 500);
        
        view.setTheme(scene, view.getMainPane());
        stage.setTitle("SUD Unpacker"); 

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

}


