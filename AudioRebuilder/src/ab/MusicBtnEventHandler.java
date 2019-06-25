package ab;

import java.util.ArrayList;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.Button;

public class MusicBtnEventHandler implements EventHandler<Event> {
	private ArrayList<Button> Btns = new ArrayList<Button>();
	private Button PauseBtn;
	@Override
	public void handle(Event e) {}
	
	public void SetPauseBtn(Button b) {
		PauseBtn = b;
		Btns.add(b);
	}
	
	public void OnStop() {
		for(Button b:Btns) {
			b.setDisable(true);
		}
		PauseBtn.setText("¡ü   Pause");

	}
	public void OnStart() {
		for(Button b:Btns) {
			b.setDisable(false);
		}
		PauseBtn.setText("¡ü   Pause");
	}
	
	public ArrayList<Button> GetListenBtns(){
		return Btns;
	}
	
}
