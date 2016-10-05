package mygame;
import com.jme3.system.AppSettings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

public class Main {
  private static final String version = "0.5 - Alpha";
  private static final String statePath = "state";
  private State s;
  private Engine e;
  public boolean showSettings = true;

  public static void main(String[] args) {
    new Main();
  }

  public Main() {
    s = new State();
    initStateFile();
    initBackgroundTrack();
    initNewLevel();
  }

  private void initNewLevel() {
    e = new Engine(this);
    // disable basic logging
    Logger.getLogger("").setLevel(Level.SEVERE);
    // applications settings
    AppSettings settings = new AppSettings(true);
    e.setShowSettings(showSettings);
    settings.setWidth(800);
    settings.setHeight(600);
    settings.setTitle("lonkz");
    settings.setVSync(true);
    settings.setFullscreen(false);
    settings.setSamples(0);
    settings.setSettingsDialogImage("/Textures/back.png");
    e.setSettings(settings);
    e.start();

    // THIS SHOULD ALL BE SERIALIZED AND SAVED
  }

  private void initBackgroundTrack(){
    if(!showSettings)
      return;
    try {
        // From file
        File f = new File("assets/Sounds/background.mid");
        Sequence sequence = MidiSystem.getSequence(f);
        // Create a sequencer for the sequence
        Sequencer sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequencer.setSequence(sequence);
        // Start playing
        sequencer.start();
    } catch (Exception e){
      System.out.println("Background sound failed!");
    }
  }

  private void initStateFile() {
    File f = new File(statePath);
    if (f.exists()) {
      return;
    }
    try {
      FileOutputStream fileOut =
              new FileOutputStream(statePath);
      ObjectOutputStream out =
              new ObjectOutputStream(fileOut);
      out.writeObject(s);
      out.close();
      fileOut.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void resetState() {
    s = new State();
  }

  public String getVersion() {
    return version;
  }

  public int getLevel() {
    return s.level;
  }

  // completed the level and proceeds to next level
  public void nextLevel() {
    s.level++;
    trySave();
  }

  // died and lost the level - the level is reset
  public void retryLevel() {
    trySave();
  }

  public void trySave() {
    File f = new File(statePath);
    if (f.exists()) {
      int level = load().level;
      if (s.level > level) {
        save();
      }
    }
  }

  private void save() {
    // serialize
    try {
      FileOutputStream fileOut =
              new FileOutputStream(statePath);
      ObjectOutputStream out =
              new ObjectOutputStream(fileOut);
      out.writeObject(s);
      out.close();
      fileOut.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void tryLoad() {
    s = load();
  }

  private State load() {
    // deserialize
    State state = null;
    try {
      FileInputStream fileIn =
              new FileInputStream(statePath);
      ObjectInputStream in = new ObjectInputStream(fileIn);
      state = (State) in.readObject();
      in.close();
      fileIn.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return state;
  }
}