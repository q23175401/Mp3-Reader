package ab;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class AudioSelector extends Application{
	public static Stage MainStage;
	public static Scene MainScene;
	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage mainStage) throws Exception {
		MainStage = mainStage;
		Parent content;
		FXMLLoader loader = new FXMLLoader(this.getClass().getResource("../Scenes/AudioShowScene.fxml"));
		try {
			content = loader.load();
		}catch(Exception e) {
			e.printStackTrace();
			content = new Label("Load Failed!");
		}
		MainScene = new Scene(content);
		MainStage.setMinWidth(616);
		MainStage.setMinHeight(939);
		MainStage.setScene(MainScene);
		MainStage.setTitle("Simple Mp3 Reader");
		MainStage.show();
	}
}
