package mygame;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class Menu extends Engine implements ScreenController {

  private Engine e = getApplication();

  public void bind(Nifty nifty, Screen screen) {
    this.nifty = nifty;
  }

  public void onStartScreen() {
    // do nothing
  }

  public void onEndScreen() {
    // do nothing
  }

  public void play() {
    nifty.fromXml("extra/story.xml", "start");
  }

  public void newGame() {
    e.newGame();
  }

  public void next() {
    e.nextLevel();
  }

  public void retry() {
    e.retryLevel();
  }

  public void load() {
    e.load();
  }

  public void controls() {
    nifty.fromXml("extra/controls.xml", "start");
  }

  public void backToMenu() {
    nifty.fromXml("extra/menu.xml", "start");
  }

  public void exit() {
    System.exit(0);
  }

  public void toMenuItem() {
    nifty.fromXml("extra/menu.xml", "start");
  }
}
