package mygame;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.objects.PhysicsCharacter;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.font.Rectangle;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import de.lessvoid.nifty.Nifty;
import java.io.File;
import java.util.Random;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

public class Engine extends SimpleApplication {

  public boolean inMenu;
  public Nifty nifty;
  private Main m;
  private static Engine staticApplication;
  private AudioNode audioPaint, audioSmash, audioDeath, audioBackground;
  private Random r = new Random();
  private CompoundCollisionShape sceneShape;
  private NiftyJmeDisplay niftyDisplay;
  private BulletAppState bulletAppState;
  private PhysicsCharacter player;
  private Vector3f walkDirection;
  private Node shootables, smashed;
  private Geometry mark;
  private Material whiteMat;
  private BitmapText ch, smashedTxt, paintedTxt, orbsLeftTxt, levelTxt;
  private BitmapFont fnt;
  private Sequencer seqrPaint = null, seqrSmash = null, seqrDeath = null;
  private Sequence seqPaint = null, seqSmash = null, seqDeath = null;
  private boolean mappingsDeleted = false;
  private boolean deathPlayed, placeHitSphere;
  private boolean left, right, up, down, diedAlready;
  private int blockName, paintsLeft, smashesLeft;
  private int level, orbsLeft, blocksAmount;
  private static final int size = 20; // must be an even number
  private static final int height = 5;
  private static final int blockRespawnTime = 5000;
  private static final int blockPaintTime = 10000;
  private static final int deathFallDist = -100;
  private static final Vector3f startLoc = new Vector3f(19, 5, 19);
  private int[][][] blocks = new int[size][height][size];

  public Engine() {
    //staticApplication = this;
  }

  public static Engine getApplication() {
    if (staticApplication == null) {
      staticApplication = new Engine();
    }
    return staticApplication;
  }

  public Engine(Main m) {
    this.m = m;
    staticApplication = this;
  }

  public void simpleInitApp() {
    initFields();
//    showGui("splashScreen.xml");
    initEngine();
  }

  public void initEngine() {
    initCollision();
    initSteering();
    initMark();
    //initAudio();
    initBlocksArray();
    createBlocks();
    initBorders();
    backFromMenu();
    initSky();
    initCam();
    updateRoot();
  }
  
  private void initCam() {
    // top/bottom and right/left shuuld be each other's negation
    // higher values makes the world appear further away
    cam.setFrustumNear(0.6f);
    cam.setFrustumTop(0.4f);
    cam.setFrustumBottom(-0.4f);
    cam.setFrustumLeft(-0.5f);
    cam.setFrustumRight(0.5f);
  }
  
  private void initFields() {
    inMenu = false;
    deathPlayed = false;
    placeHitSphere = false;
    walkDirection = new Vector3f();
    left = false;
    right = false;
    up = false;
    down = false;
    diedAlready = false;
    blockName = 1;
    paintsLeft = 20;
    smashesLeft = 8;

    level = m.getLevel();
    orbsLeft = level * 2;
    blocksAmount = 50 + level * 50;

    fnt = assetManager.loadFont("Interface/Fonts/Default.fnt");
    player = new PhysicsCharacter(new CapsuleCollisionShape(0.8f, 2f, 1), .1f);
    
    whiteMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    whiteMat.setColor("Color", ColorRGBA.White);

    shootables = new Node("Shootables");
    smashed = new Node("Smashed");
    rootNode.detachAllChildren();
  }

  private void updateRoot() {
    rootNode.updateGeometricState();
  }

  private void destroySphere(Vector3f loc) {
    destroySphereFire(loc);
    destroySphereDebris(loc);
  }
  
  private void destroySphereFire(Vector3f loc){
    ParticleEmitter  fire = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
    Material mat_red = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
    mat_red.setTexture("m_Texture", assetManager.loadTexture("Textures/flame.png"));
    fire.setMaterial(mat_red);
    fire.setImagesX(2);
    fire.setImagesY(2); // 2x2 texture animation
    fire.setEndColor(new ColorRGBA(1f, 0f, 0f, 1f));   // red
    fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
    //    fire.setStartVel(new Vector3f(0, 2, 0));
    fire.setStartSize(0.5f);
    fire.setEndSize(1f);
    //    fire.setGravity(1);
    fire.setLowLife(0.5f);
    fire.setHighLife(5f);
    //fire.setVariation(0.3f);
    fire.setLocalTranslation(loc);
    rootNode.attachChild(fire);
  }
  
  private void destroySphereDebris(Vector3f loc){
    ParticleEmitter debris = new ParticleEmitter("Debris", ParticleMesh.Type.Triangle, 10);
    Material debris_mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
    debris_mat.setTexture("m_Texture", assetManager.loadTexture("Textures/debris.png"));
    debris.setMaterial(debris_mat);
    debris.setImagesX(3);
    debris.setImagesY(3); // 3x3 texture animation
    debris.setRotateSpeed(4);
    debris.setSelectRandomImage(true);
    //    debris.setStartVel(new Vector3f(0, 4, 0));
    debris.setStartColor(new ColorRGBA(1f, 1f, 1f, 1f));
    //    debris.setGravity(6f);
    //debris.setVariation(.60f);
    rootNode.attachChild(debris);
    debris.setLocalTranslation(loc);
    debris.emitAllParticles();
  }

  public void load() {
    File f = new File("state");
    if (f.exists()) {
      m.tryLoad();
      initEngine();
    } else {
      initEngine();
    }
  }

  public void newGame() {
    m.resetState();
    level = m.getLevel();
    initEngine();
    System.gc();
  }

  public void nextLevel() {
    m.nextLevel();
    level = m.getLevel();
    initEngine();
    System.gc();
  }

  public void retryLevel() {
    m.retryLevel();
    initEngine();
    System.gc();
  }

  private void initAudio() {
    try{
        // paint
        File f = new File("assets/Sounds/paint.mid");
        seqPaint = MidiSystem.getSequence(f);
        seqrPaint = MidiSystem.getSequencer();
        seqrPaint.open();
        seqrPaint.setSequence(seqPaint);

        //smash
        f = new File("assets/Sounds/smash.mid");
        seqSmash = MidiSystem.getSequence(f);
        seqrSmash = MidiSystem.getSequencer();
        seqrSmash.open();
        seqrSmash.setSequence(seqSmash);

        // death
        f = new File("assets/Sounds/death.mid");
        seqDeath = MidiSystem.getSequence(f);
        seqrDeath = MidiSystem.getSequencer();
        seqrDeath.open();
        seqrDeath.setSequence(seqDeath);
    } catch (Exception e){
      System.out.println("Sound failed!");
    }

    seqrPaint.start();
    seqrSmash.start();
    seqrDeath.start();
  }

  public void backFromMenu() {
    inMenu = false;
    showGui("gui.xml");

    initCrossHairs();
    initGui();
    // enable mouse
    flyCam.setDragToRotate(false);
    // enable keys
    initSteering();
  }

  private void initSky() {
    viewPort.setBackgroundColor(new ColorRGBA(0.7f, 0.8f, 1f, 1f));
  }

  private void youWin() {
    try {  
      // the mapping may or may not exist
      // find a nicer way to handle this
      inputManager.deleteMapping("Menu");
    } catch (Exception e) {
        
    }
    showGui("next.xml");
  }

  private void youLose() {
    try {  
      // the mapping may or may not exist
      // find a nicer way to handle this
      inputManager.deleteMapping("Menu");
    } catch (Exception e) {
        
    }
    showGui("retry.xml");
  }

  private void setupMenu() {
    inMenu = true;
    showGui("menu.xml");
    // disable the fly cam
    //flyCam.setDragToRotate(true);
    // remove keys
    String mappings[] = {"Shoot", "Smash", "Lefts", "Rights", "Ups", "Downs", "Jumps"};
    for (String s : mappings) {
      inputManager.deleteMapping(s);
    }
  }

  public void showGui(String name) {
    flyCam.setDragToRotate(true);
    guiNode.detachAllChildren();
    if (guiViewPort.getProcessors().contains(niftyDisplay)) {
      guiViewPort.removeProcessor(niftyDisplay);
    }
    niftyDisplay = new NiftyJmeDisplay(
            assetManager, inputManager, audioRenderer, guiViewPort);
    nifty = niftyDisplay.getNifty();
    nifty.fromXml("extra/" + name, "start");
    // attach the nifty display to the gui view port as a processor
    guiViewPort.addProcessor(niftyDisplay);
  }

  @Override
  public void simpleUpdate(float tpf) {
    Vector3f camDir = cam.getDirection().clone().multLocal(0.15f);
    Vector3f camLeft = cam.getLeft().clone().multLocal(0.1f);
    
    // do not move vertically
    camDir.y = 0;
    camLeft.y = 0;
    updateWalkDirection(camLeft, camDir);
    updateFalling();

    // make audio follow cam
    listener.setLocation(cam.getLocation());
    listener.setRotation(cam.getRotation());
  }
  
  private void updateWalkDirection(Vector3f camLeft, Vector3f camDir) {
    walkDirection.set(0, 0, 0);
    if (left) {
      walkDirection.addLocal(camLeft);
    }
    if (right) {
      walkDirection.addLocal(camLeft.negate());
    }
    if (up) {
      walkDirection.addLocal(camDir);
    }
    if (down) {
      walkDirection.addLocal(camDir.negate());
    }
    player.setWalkDirection(walkDirection);
    cam.setLocation(player.getPhysicsLocation());
  }
  
  private void updateFalling(){
    // play falling sound
    float fallSoundDist = -2;
    if (cam.getLocation().y < fallSoundDist && !deathPlayed) {
      deathPlayed = true;
      //audioRenderer.playSource(audioDeath);
    }

    // lose if you fall too far.
    if (cam.getLocation().y < deathFallDist && !diedAlready) {
      diedAlready = true;
      youLose();
    }
  }

  private void initCollision() {
    // Set up Physics
    if (stateManager.hasState(bulletAppState)) {
      stateManager.detach(bulletAppState);
    }
    bulletAppState = new BulletAppState();
    bulletAppState.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
    stateManager.attach(bulletAppState);

    player.setJumpSpeed(15);
    player.setFallSpeed(30);
    player.setGravity(30);
    player.setPhysicsLocation(startLoc);

    rootNode.attachChild(shootables);
    bulletAppState.getPhysicsSpace().add(player);
    // throws NPE, but without it you fall through the floor
    //bulletAppState.getPhysicsSpace().add(shootables);
  }

  private void initGui() {
    guiPainted();
    guiSmashed();
    guiOrbsLeft();
    guiLevel();
    guiFps();
  }
  
  private void guiPainted() {
    paintedTxt = new BitmapText(fnt, false);
    paintedTxt.setBox(new Rectangle(2, -533,
            settings.getWidth(), settings.getHeight()));
    paintedTxt.setSize(fnt.getPreferredSize() * 1f);
    paintedTxt.setColor(ColorRGBA.White);
    paintedTxt.setText("PAINTS \nLEFT: \n" + paintsLeft);
    paintedTxt.setLocalTranslation(0, settings.getHeight(), 0);
    guiNode.attachChild(paintedTxt);
  }

  private void guiSmashed() {
    smashedTxt = new BitmapText(fnt, false);
    smashedTxt.setBox(new Rectangle(180, -533,
            settings.getWidth(), settings.getHeight()));
    smashedTxt.setSize(fnt.getPreferredSize() * 1f);
    smashedTxt.setColor(ColorRGBA.White);
    smashedTxt.setText("SMASHES \nLEFT: \n" + smashesLeft);
    smashedTxt.setLocalTranslation(0, settings.getHeight(), 0);
    guiNode.attachChild(smashedTxt);
  }

  private void guiOrbsLeft() {
    orbsLeftTxt = new BitmapText(fnt, false);
    orbsLeftTxt.setBox(new Rectangle(360, -533,
            settings.getWidth(), settings.getHeight()));
    orbsLeftTxt.setSize(fnt.getPreferredSize() * 1f);
    orbsLeftTxt.setColor(ColorRGBA.White);
    orbsLeftTxt.setText("ORBS \nLEFT: \n" + orbsLeft);
    orbsLeftTxt.setLocalTranslation(0, settings.getHeight(), 0);
    guiNode.attachChild(orbsLeftTxt);
  }

  private void guiLevel() {
    levelTxt = new BitmapText(fnt, false);
    levelTxt.setBox(new Rectangle(540, -533,
            settings.getWidth(), settings.getHeight()));
    levelTxt.setSize(fnt.getPreferredSize() * 1f);
    levelTxt.setColor(ColorRGBA.White);
    levelTxt.setText("\nLEVEL: \n" + level);
    levelTxt.setLocalTranslation(0, settings.getHeight(), 0);
    guiNode.attachChild(levelTxt);
  }

  private void guiFps() {
    fpsText.setBox(new Rectangle(720, -533,
            settings.getWidth(), settings.getHeight()));
    fpsText.setSize(fnt.getPreferredSize() * 1f);
    fpsText.setColor(ColorRGBA.White);
    fpsText.setLocalTranslation(0, settings.getHeight(), 0);
    guiNode.attachChild(fpsText);
  }

  private int setBlocks(int i) {
    if (i == 0) {
      return 0;
    }
    int x = r.nextInt(size);
    int y = r.nextInt(height);
    int z = r.nextInt(size);
    // do no override blocks
    if (blocks[x][y][z] == 1
            || (x >= 9 && x <= 11)
            && (z >= 9 && z <= 11)) {
      return setBlocks(i);
    } else {
      blocks[x][y][z] = 1;
      return setBlocks(i - 1);
    }
  }

  private int setSpheres(int i) {
    if (i == 0) {
      return 0;
    }
    int x = r.nextInt(size);
    int y = r.nextInt(height);
    int z = r.nextInt(size);
    // do no override blocks
    if (blocks[x][y][z] == 1
            || blocks[x][y][z] == 2
            || (x >= 7 && x <= 13)
            && (z >= 7 && z <= 13)) {
      return setSpheres(i);
    } else {
      blocks[x][y][z] = 2;
      return setSpheres(i - 1);
    }
  }

  private void zeroBlocksArray() {
    for (int x = 0; x < size; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < size; z++) {
          blocks[x][y][z] = 0;
        }
      }
    }
  }

  private void makeStartZone() {
    for (int x = size / 2 - 1; x <= size / 2 + 1; x++) {
      for (int z = size / 2 - 1; z <= size / 2 + 1; z++) {
        blocks[x][0][z] = 1;
      }
    }
  }
  
  private void initBlocksArray() {
    zeroBlocksArray();
    makeStartZone();
    setBlocks(blocksAmount);
    setSpheres(orbsLeft);
  }

  private void createBlocks() {
    for (int x = 0; x < blocks.length; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < blocks.length; z++) {
          if (blocks[x][y][z] == 1) {
            makeBlock(2 * x, 2 * y, 2 * z);
          } else if (blocks[x][y][z] == 2) {
            makeBall(2 * x, 2 * y, 2 * z);
          }
        }
      }
    }
  }

  private void makeBall(float a, float b, float c) {
    shootables.attachChild(makeSphere("ball" + blockName++, a, b, c));
  }

  private void makeBlock(float a, float b, float c) {
    shootables.attachChild(makeCube("block" + blockName++, a, b, c));
  }

  private void initSteering() {
    // delete bindings
    if (!mappingsDeleted) {
      String[] bindings = {"SIMPLEAPP_Exit",
        "SIMPLEAPP_CameraPos",
        "SIMPLEAPP_Memory"};
      for (String s : bindings) {
        inputManager.deleteMapping(s);
      }
      mappingsDeleted = true;
    }
    // mouse
    inputManager.addMapping("Shoot", new MouseButtonTrigger(0));
    inputManager.addMapping("Smash", new MouseButtonTrigger(1));
    // keyboard
    inputManager.addMapping("Menu", new KeyTrigger(KeyInput.KEY_ESCAPE));
    inputManager.addMapping("Lefts", new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping("Rights", new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping("Ups", new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping("Downs", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Jumps", new KeyTrigger(KeyInput.KEY_SPACE));
    // add mappings to listener
    inputManager.addListener(actionListener, 
      "Smash", "Shoot", "Menu", "Lefts",
      "Rights", "Ups", "Downs", "Jumps");
  }

  private void shootablesAttach(Geometry g) {
    shootables.attachChild(g);
  }

  private void shootablesDetach(Geometry g) {
    shootables.detachChild(g);
  }

  private void makePaintThread(Geometry geom) {
      final Geometry g = geom;
      new Thread(new Runnable() {

        public void run() {
            // PLAY
            String name = g.getName();
            g.setName("hit" + g.getName());
            Material randomMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            randomMat.setColor("Color", ColorRGBA.randomColor());
            g.setMaterial(randomMat);
            paintedTxt.setText("PAINTS \nLEFT: \n" + --paintsLeft);

          try {
            Thread.currentThread().sleep(blockPaintTime);
          } catch (Exception e) {
            System.out.println("Sleeping failed!");
          }
            g.setName(name);
            g.setMaterial(whiteMat);
            paintedTxt.setText("PAINTS \nLEFT: \n" + ++paintsLeft);
        }
      }).start();
  }

  private void makeRespawnThread(Geometry geom, CollisionResult result) {
    final Geometry g = geom;
    final CollisionResult cr = result;
    // You have to call bulletAppState.getPhysicsSpace().remove(box);
    // somewhere to remove the physics control from the physics space.
    // the appstate is already updated automatically,
    // so doing the update thing is not good.
    new Thread(new Runnable() {  
        public void run() {
          try {
            //bulletAppState.getPhysicsSpace().remove(landscape);
            //shootablesDetach(g);
            //smashed.attachChild(g);
            //landscape = new PhysicsNode(shootables, sceneShape, 0);
            //bulletAppState.getPhysicsSpace().add(landscape);
            bulletAppState.getPhysicsSpace().remove(cr);
            smashedTxt.setText("SMASHES \nLEFT: \n" + --smashesLeft);
          } catch (Exception e) {
            System.out.println("disappear");
            e.printStackTrace();
          }

          try {
            Thread.currentThread().sleep(blockRespawnTime);
          } catch (Exception e) {
            System.out.println("Sleeping failed!");
          }

          try {
            shootablesAttach(g);
            smashed.detachChild(g); // perhaps not necessary?
            bulletAppState.getPhysicsSpace().add(cr);
            smashedTxt.setText("SMASHES \nLEFT: \n" + ++smashesLeft);
          } catch (Exception e) {
            System.out.println("appear");
          }
          }
      }).start();
  }

  private Geometry makeSphere(String name, float x, float y, float z) {
    Sphere sphere = new Sphere(10, 10, 1f);
    Geometry ball = new Geometry(name, sphere);
    ball.setLocalTranslation(x, y, z);
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.White);
    //mat.setColor("Color", ColorRGBA.randomColor());
    ball.setMaterial(mat);
    ball.setCullHint(CullHint.Never);
    return ball;
  }

  private Geometry makeCube(String name, float x, float y, float z) {
    Vector3f center = new Vector3f(x, y, z);
    Box box = new Box(center, 1, 1, 1);
    Geometry cube = new Geometry(name, box);
    //cube.setQueueBucket(Bucket.Transparent); // no clip
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    //mat1.getAdditionalRenderState().setDepthTest(false); // no clip
    mat.setColor("Color", ColorRGBA.White);
    //mat.setColor("Color", ColorRGBA.randomColor());
    cube.setMaterial(mat);
    cube.setCullHint(CullHint.Never);
    return cube;
  }

  private void initBorders() {
    Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat.setColor("Color", ColorRGBA.Black);
    // under the blocks
    // place in level 1 and 2 -- doesn't scale with world size
    if (level == 1 || level == 2) {
      Box box = new Box(new Vector3f(19, -1, 19), 20.2f, .1f, 20.2f);
      Geometry floor = new Geometry("floor1", box);
      floor.setMaterial(mat);
      shootables.attachChild(floor);
    }

    // over the blocks -- not fun... might be implemented later
    //if (level == 2 || level == 3) {
    //Box box3 = new Box(new Vector3f(19, 9, 19), 20.2f, .1f, 20.2f);
    //Geometry floor3 = new Geometry("floor3", box3);
    //floor3.setMaterial(mat);
    //shootables.attachChild(floor3);
    //}
  }

  private void initMark() {
    Sphere sphere = new Sphere(30, 30, 0.2f);
    mark = new Geometry("BOOM!", sphere);
    Material mark_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mark_mat.setColor("Color", ColorRGBA.Red);
    mark.setMaterial(mark_mat);
  }

  private void initCrossHairs() {
    //guiNode.detachAllChildren();
    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    ch = new BitmapText(guiFont, false);
    ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    ch.setText("+"); // crosshairs
    ch.setLocalTranslation( // center
            settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
            settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
    guiNode.attachChild(ch);
  }
  
  // the action listener needs refactoring
  private ActionListener actionListener = new ActionListener() {
    @Override
    public void onAction(String name, boolean keyPressed, float tpf) {
      if (name.equals("Lefts")) {
        left = keyPressed;
      } else if (name.equals("Rights")) {
        right = keyPressed;
      } else if (name.equals("Ups")) {
        up = keyPressed;
      } else if (name.equals("Downs")) {
        down = keyPressed;
      } else if (name.equals("Jumps") && keyPressed) {
        player.jump();
      } else if (name.equals("Menu") && !keyPressed) {
        if (inMenu) {  // back from menu
          backFromMenu();
          initSteering();
        } else {  // enter menu
          setupMenu();
        }
      }

      // get rootNode ready for picking
      rootNode.updateGeometricState();
      shootables.updateGeometricState();

      // SHOOT - or - SMASH
      if ((name.equals("Shoot") || name.equals("Smash")) && !keyPressed) {
        // 1. Reset results list.
        CollisionResults results = new CollisionResults();
        // 2. Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        // 3. Collect intersections between Ray and Shootables in results list.
        shootables.collideWith(ray, results);
        // 4. Print the results.
        for (int i = 0; i < results.size(); i++) {
          CollisionResult c = results.getCollision(i);
          if (c.getGeometry() == null) {
            //System.out.println("Picking failed...");
            return;
          }
        }
        // 5. Use the results (we mark the hit object)
        if (results.size() > 0) {
          // The closest collision point is what was truly hit:
          CollisionResult closest = results.getClosestCollision();
          colorBox(name, closest);
          // Let's interact - we mark the hit with a red dot.
          mark.setLocalTranslation(closest.getContactPoint());
          if (placeHitSphere) {
            rootNode.attachChild(mark);
          }
        } else {
          // No hits? Then remove the red mark.
          rootNode.detachChild(mark);
        }
      }
    }

    private void colorBox(String name, CollisionResult closest) {
      Geometry g = closest.getGeometry();
      // do not color floor
      if (g.getName().substring(0, 5).equals("floor")) {
        return;
      }
      // destroy sphere and dec spheresLeft
      if (g.getName().substring(0, 4).equals("ball")) {
        // new unique name that starts with hit
        g.setName("hit" + g.getName());
        // set mat and update orbsLeft
        orbsLeftTxt.setText("ORBS \nLEFT: \n" + --orbsLeft);
        destroySphere(g.getLocalTranslation());
        shootables.detachChild(g);
        rootNode.detachChild(g);
      }
      if (orbsLeft == 0) {
        youWin();
      }
      if (name.equals("Shoot") && paintsLeft > 0
              && !g.getName().substring(0, 3).equals("hit")) {
        makePaintThread(g);
      } else if (name.equals("Smash") && smashesLeft > 0) {
        makeRespawnThread(g, closest);
      }
    }
  };
}