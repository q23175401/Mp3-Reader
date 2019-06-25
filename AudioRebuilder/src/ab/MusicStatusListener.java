package ab;

import java.util.ArrayList;

public class MusicStatusListener {
	public ArrayList<MusicBtnEventHandler>MusicEvent = new  ArrayList<MusicBtnEventHandler>();
	
	public void AddMusicEventListener(MusicBtnEventHandler m) {
		MusicEvent.add(m);
	}
	
	public void MusicStop() {
		if(MusicEvent!=null) {
			for (MusicBtnEventHandler m:MusicEvent) {
				m.OnStop();
			}
		}
	}
	
	public void MusicStart() {
		if(MusicEvent!=null) {
			for (MusicBtnEventHandler m:MusicEvent) {
				m.OnStart();
			}
		}
	}
	
}
