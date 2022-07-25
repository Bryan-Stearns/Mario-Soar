package manager;

import java.util.ArrayList;

//import javax.lang.model.util.ElementScanner14;

import com.optum.ect.soar.SoarLinkAbstract;
import com.optum.ect.soar.SoarMemObj;
import com.optum.ect.soar.SoarValueType;

import model.EndFlag;
import model.GameObject;
import model.Map;
import model.brick.Brick;
import model.enemy.Enemy;
import model.enemy.KoopaTroopa;
import model.hero.Mario;
import model.prize.BoostItem;
import model.prize.FireFlower;
import model.prize.OneUpMushroom;
import model.prize.SuperMushroom;
import sml.Agent;
import sml.Identifier;
import sml.WMElement;

public class MarioSoarLink extends SoarLinkAbstract {
    private GameEngine engine;
    private boolean keyPressed_left = false,
                    keyPressed_right = false,
                    keyPressed_A = false,
                    keyPressed_B = false,
                    messageGiven = false;
    private boolean status = true;
    private String error_message = "";

    public MarioSoarLink(String agentFilePath, GameEngine engine) {
        // Start the soar kernel+agent using port 12121 (the default)
        super(12121);
        this.kernel.SetAutoCommit(false);

        // Remember the engine
        this.engine = engine;

        // Set up the agent
        status = agent.LoadProductions(agentFilePath);
        if (!status) {
            error_message = "Error loading productions from path: "+agentFilePath;
            System.out.println("ERROR: "+error_message);
        }

        agent.SetBlinkIfNoChange(false);
    }
    
    public boolean getWasError() { return !status; }
    public String getErrorMessage() { return error_message; }

    public void runAgentForever_nonBlocking() {
        final Agent a = agent; // the final is effectively redundant due to the nature of the Soar agent, but good practice
        Thread t = new Thread(new Runnable() {
            public void run() { 
              a.RunSelfForever();
            };
        });
        t.start();
    }

    public void resetAgent() {
        cleanInputWMEs();
        agent.InitSoar();
    }

    /*private String getWmeTreeString(WMElement wme) {
        String retVal = "";
        String idStr = wme.GetIdentifierName();
        String attrStr = wme.GetAttribute();
        String valStr = wme.GetValueAsString();
        Identifier id = wme.ConvertToIdentifier();
        int childrenCount = id.GetNumberChildren();
        
        // Iterate through all children and print them along with their descendants too
        //SWIGTYPE_p_std__listT_sml__WMElement_p_t__iterator iter = id.GetChildrenBegin();
        //while (iter.)
        for (int i=0; i<childrenCount; ++i) {
            WMElement child = id.GetChild(i);
            String idName = child.GetIdentifierName();
            String attrName = child.GetAttribute();
            String valName = child.GetValueAsString();
            retVal += child.GetIdentifierName()+" ^"+child.GetAttribute()+" "+child.GetValueAsString() + "\n";
            
            if (child.IsIdentifier()) {
                retVal += getWmeTreeString(child.ConvertToIdentifier())+"\n";
            }
        }

        return retVal;
    }

    public String getInputLinkString() {
        WMElement wme = getInputLink();

        return getWmeTreeString(wme);
    }*/

    private SoarMemObj makeBrickSoarObj(Brick brick) {
        SoarMemObj retVal = new SoarMemObj("brick-obj", SoarValueType.OBJECT);
        Mario mario = engine.getMapManager().getMario();

        retVal.add_aug("type", brick.getType());
        retVal.add_aug("is-breakable", (brick.isBreakable() ? "true" : "false"));
        retVal.add_aug("x-absolute", (int)brick.getX());
        retVal.add_aug("y-absolute", (int)brick.getY());
        retVal.add_aug("x-relative", (int)mario.getRelativeX(brick.getX()));
        retVal.add_aug("y-relative", (int)mario.getRelativeY(brick.getY()));

        return retVal;
    }

    private SoarMemObj makeEnemySoarObj(Enemy enemy) {
        SoarMemObj retVal = new SoarMemObj("enemy-obj", SoarValueType.OBJECT);
        Mario mario = engine.getMapManager().getMario();

        String type = "goomba";
        if (enemy instanceof KoopaTroopa)
            type = "koopatroopa";

        retVal.add_aug("type", type);
        retVal.add_aug("x-absolute", (int)enemy.getX());
        retVal.add_aug("y-absolute", (int)enemy.getY());
        retVal.add_aug("x-relative", (int)mario.getRelativeX(enemy.getX()));
        retVal.add_aug("y-relative", (int)mario.getRelativeY(enemy.getY()));
        retVal.add_aug("x-speed", enemy.getVelX());
        retVal.add_aug("y-speed", enemy.isFalling() ? -enemy.getVelYAbs() : enemy.getVelYAbs());

        return retVal;
    }

    private SoarMemObj makeBoostSoarObj(BoostItem boost) {
        SoarMemObj retVal = new SoarMemObj("boost-obj", SoarValueType.OBJECT);
        Mario mario = engine.getMapManager().getMario();

        String type = null;
        if (boost instanceof SuperMushroom)
            type = "super-mushroom";
        else if (boost instanceof FireFlower)
            type = "fire-flower";
        else if (boost instanceof OneUpMushroom)
            type = "one-up-mushroom";
        
        if (type == null)
            return null;

        retVal.add_aug("type", type);
        retVal.add_aug("x-absolute", (int)boost.getX());
        retVal.add_aug("y-absolute", (int)boost.getY());
        retVal.add_aug("x-relative", (int)mario.getRelativeX(boost.getX()));
        retVal.add_aug("y-relative", (int)mario.getRelativeY(boost.getY()));
        retVal.add_aug("x-speed", boost.getVelX());
        retVal.add_aug("y-speed", boost.isFalling() ? -boost.getVelYAbs() : boost.getVelYAbs());

        return retVal;
    }

    private SoarMemObj makeEndFlagSoarObj(EndFlag endFlag) {
        SoarMemObj retVal = new SoarMemObj("endflag-obj", SoarValueType.OBJECT);
        Mario mario = engine.getMapManager().getMario();

        retVal.add_aug("x-absolute", (int)endFlag.getX());
        retVal.add_aug("y-absolute", (int)endFlag.getY());
        retVal.add_aug("x-relative", (int)mario.getRelativeX(endFlag.getX()));
        retVal.add_aug("y-relative", (int)mario.getRelativeY(endFlag.getY()));
        //retVal.add_aug("height", (int)endFlag.getBounds().getHeight());

        return retVal;
    }

    public void addInput_mario(Mario mario, double remainingTime) {
        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                marioObj = new SoarMemObj("mario", SoarValueType.OBJECT),
                marioBody = new SoarMemObj("body", SoarValueType.OBJECT),
                marioHud = new SoarMemObj("hud", SoarValueType.OBJECT);
        ilTree.add_aug("mario", marioObj);
        marioObj.add_aug("body", marioBody);
        marioObj.add_aug("hud", marioHud);

        // Add HUD values
        marioHud.add_aug("time", remainingTime);
        marioHud.add_aug("points", mario.getPoints());
        marioHud.add_aug("lives", mario.getRemainingLives());

        // Add body values
        marioBody.add_aug("x-absolute", (int)mario.getX());
        marioBody.add_aug("y-absolute", (int)mario.getY());
        marioBody.add_aug("x-speed", mario.getVelX());
        marioBody.add_aug("y-speed", mario.getVelYAbs() * (mario.isFalling() ? 1.0 : -1.0));
        marioBody.add_aug("is-super", (mario.isSuper() ? "true" : "false"));
        marioBody.add_aug("is-fire", (mario.getMarioForm().isFire() ? "true" : "false"));
        marioBody.add_aug("height", mario.getDimension().height);
        marioBody.add_aug("width", mario.getDimension().width);

        // Add this tree to the input link
        addInputWMEs(ilTree);
    }

    public void addInput_touching(ArrayList<GameObject> marioCollidersTop, ArrayList<GameObject> marioCollidersBottom,
            ArrayList<GameObject> marioCollidersLeft, ArrayList<GameObject> marioCollidersRight) {
        addInput_touching_named("touching-top", marioCollidersTop);
        addInput_touching_named("touching-bottom", marioCollidersBottom);
        addInput_touching_named("touching-left", marioCollidersLeft);
        addInput_touching_named("touching-right", marioCollidersRight);

        kernel.CommitAll();
    }

    private void addInput_touching_named(String attrName, ArrayList<GameObject> colliderList) {

        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                topObj = new SoarMemObj(attrName, SoarValueType.OBJECT);
        ilTree.add_aug(attrName, topObj);

        // Add each brick
        for (GameObject obj : colliderList) {
            if (obj instanceof Enemy) {
                topObj.add_aug("enemy", makeEnemySoarObj((Enemy) obj));
            }
            else if (obj instanceof Brick) {
                topObj.add_aug("brick", makeBrickSoarObj((Brick) obj));
            }
        }

        addInputWMEs(ilTree);
    }

    public void addInput_enemies(ArrayList<Enemy> enemyList) {
        if (enemyList.size() == 0)
            return;
            
        // Reference mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                enemiesObj = new SoarMemObj("enemies", SoarValueType.OBJECT);
        ilTree.add_aug("enemies", enemiesObj);

        // Add each brick
        for (Enemy enemy : enemyList) {
            if (!map.isWithinCamera(enemy))
                continue;

            SoarMemObj enemyObj = makeEnemySoarObj(enemy);
            enemiesObj.add_aug("enemy", enemyObj);
        }

        // Add this tree to the input link
        addInputWMEs(ilTree);
    }

    public void addInput_bricks(ArrayList<Brick> brickList) {
        // Reference mario for relative coordinates
        Mario mario = engine.getMapManager().getMario();
        Map map = engine.getMapManager().getMap();

        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                enemiesObj = new SoarMemObj("bricks", SoarValueType.OBJECT);
        ilTree.add_aug("bricks", enemiesObj);

        // Add each brick
        for (Brick b : brickList) {
            if (!map.isWithinCamera(b))
                continue;

            int relX = (int)mario.getRelativeX(b.getX());
            int relY = (int)mario.getRelativeY(b.getY());
            // Skip bricks that are farther than 4 blocks away
            if (relX > 48*4 || relY > 48*4)
                continue;

            SoarMemObj brick = makeBrickSoarObj(b);

            // Attach this brick to the tree
            enemiesObj.add_aug("brick", brick);
        }

        // Add this tree to the input link
        addInputWMEs(ilTree);
    }

    public void addInput_powerups(ArrayList<BoostItem> pupsList) {
        if (pupsList.size() == 0)
            return;

        // Reference mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                pupsObj = new SoarMemObj("power-ups", SoarValueType.OBJECT);
        ilTree.add_aug("power-ups", pupsObj);

        // Add each brick
        for (BoostItem pup : pupsList) {
            if (!map.isWithinCamera(pup))
                continue;

            SoarMemObj pupObj = makeBoostSoarObj(pup);

            // The returned object will be null if the BoostItem is not one of the valid classes
            if (pupObj == null)
                continue;
            
            pupsObj.add_aug("power-up", pupObj);
        }

        // Add this tree to the input link
        addInputWMEs(ilTree);
    }

    public void addInput_specials(GameObject obj) { //ArrayList<GameObject> objList) {
        // Reference mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                specialsObj = new SoarMemObj("special", SoarValueType.OBJECT);
        ilTree.add_aug("special", specialsObj);

        // Add each brick
        //for (GameObject obj : objList) {
            if (!map.isWithinCamera(obj))
                return; //continue;

            SoarMemObj endObj;
            if (obj instanceof EndFlag) {
                endObj = makeEndFlagSoarObj((EndFlag)obj);
                specialsObj.add_aug("end-flag", endObj);
            }
        //}

        // Add this tree to the input link
        addInputWMEs(ilTree);
    }

    @Override
    protected void user_outputEvent(WMElement output) {
        // Make vars to track what is pressed in this output cluster
        boolean output_keyPressed_left = false,
                output_keyPressed_right = false,
                output_keyPressed_A = false,
                output_keyPressed_B = false,
                output_messageGiven = false;
        
        int numCmds = agent.GetNumberCommands();
        int numChildren = output.ConvertToIdentifier().GetNumberChildren();
        if (numCmds == 0) {
            return;
        }

        for (int i=0; i<numChildren; ++i) {
            //Identifier commandId = agent.GetCommand(i);
            Identifier commandId = output.ConvertToIdentifier().GetChild(i).ConvertToIdentifier();

			/*if (commandId.GetParameterValue("status") != null) {
				//System.out.println("Status already processed.");
				continue;
			}*/

            String commandAttr = commandId.GetAttribute();
            
            if (commandAttr.equals("message")) {
                String message = commandId.GetParameterValue("value");
                output_messageGiven = true;
                engine.setAgentMessage(message);
                if (commandId.GetParameterValue("status") == null) {
                    commandId.AddStatusComplete();
                }
                continue;
            }
            else if (commandAttr.equals("key-press")) {
                String keyCode = commandId.GetParameterValue("value");

                if (keyCode.equals("A")) {
                    output_keyPressed_A = true;
                    if (commandId.GetParameterValue("status") == null) {
                        commandId.AddStatusComplete();
                    }
                    continue;
                }
                else if (keyCode.equals("B")) {
                    output_keyPressed_B = true;
                    if (commandId.GetParameterValue("status") == null) {
                        commandId.AddStatusComplete();
                    }
                    continue;
                }
                else if (keyCode.equals("left")) {
                    output_keyPressed_left = true;
                    if (commandId.GetParameterValue("status") == null) {
                        commandId.AddStatusComplete();
                    }
                    continue;
                }
                else if (keyCode.equals("right")) {
                    output_keyPressed_right = true;
                    if (commandId.GetParameterValue("status") == null) {
                        commandId.AddStatusComplete();
                    }
                    continue;
                }
                else {
                    if (commandId.GetParameterValue("status") == null) {
                        commandId.AddStatusError();
                    }
                    continue;
                }
            }

        } // END for (int i=0, iLim=agent.GetNumberCommands(); i<iLim; ++i)


        //// Update state accordingly based on keypress changes

        // MESSAGE
        if (!messageGiven && output_messageGiven) {
            // The message text was already updated earlier, so just update the flag here
            messageGiven = true;
        }
        else if (messageGiven && !output_messageGiven) {
            // Clear message text display if no more message
            engine.setAgentMessage("");
            messageGiven = false;
        }

        // LEFT KEY
        if (!keyPressed_left && output_keyPressed_left) {
            // Press left key
            keyPressed_left = true;
            engine.receiveInput(ButtonAction.M_LEFT);
        }
        else if (keyPressed_left && !output_keyPressed_left) {
            // Release left key
            keyPressed_left = false;
            engine.receiveInput(ButtonAction.LEFT_RELEASED);
        }
        // RIGHT KEY
        if (!keyPressed_right && output_keyPressed_right) {
            // Press right key
            keyPressed_right = true;
            engine.receiveInput(ButtonAction.M_RIGHT);
        }
        else if (keyPressed_right && !output_keyPressed_right) {
            // Release right key
            keyPressed_right = false;
            engine.receiveInput(ButtonAction.RIGHT_RELEASED);
        }
        // A KEY
        if (!keyPressed_A && output_keyPressed_A) {
            // Press A key
            keyPressed_A = true;
            engine.receiveInput(ButtonAction.JUMP);
        }
        else if (keyPressed_A && !output_keyPressed_A) {
            // Release A key
            keyPressed_A = false;
            engine.receiveInput(ButtonAction.JUMP_RELEASED);
        }
        // B KEY
        if (!keyPressed_B && output_keyPressed_B) {
            // Press B key
            keyPressed_B = true;
            engine.receiveInput(ButtonAction.FIRE);
        }
        else if (keyPressed_B && !output_keyPressed_B) {
            // Release B key
            keyPressed_B = false;
        }
    }

    @Override
    protected void user_interruptEvent() {
        // If the agent code interrupts the agent, also pause the game
        engine.pauseGame("AGENT INTERRUPTED");
    }
    
}
