package manager;

import java.util.ArrayList;
import java.util.HashMap;

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
    private Identifier marioRoot, marioHud, marioBody,
                       enemiesRoot,
                       bricksRoot,
                       specialRoot;
    private HashMap<Integer, Identifier> objId_map;         // Records the input ID structs currently sent to the agent's input-link. Maps from [game-object-hash] to [sml.Identifier]
    private ArrayList<Integer> refreshedObjectHashes;       // Records the input objects that are used each cycle (any input not referenced in this list should get destroyed after all input is refreshed)

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

        // Create the top-level input parent structures
        Identifier inputId = agent.GetInputLink();
        marioRoot = inputId.CreateIdWME("mario");
        marioHud = marioRoot.CreateIdWME("hud");
        marioBody = marioRoot.CreateIdWME("body");
        enemiesRoot = inputId.CreateIdWME("enemies");
        bricksRoot = inputId.CreateIdWME("bricks");
        specialRoot = inputId.CreateIdWME("special");
        agent.Commit();

        // Set up Identifier tracking structs
        objId_map = new HashMap<Integer, Identifier>(40);
        refreshedObjectHashes = new ArrayList<Integer>(40);
    }

    /** TODO
     * Add "^touching-... <id>" augs to bricks touching each other
     * // Add "^touching-bottom <id>" augs to enemies touching bricks
     */
    
    public boolean getWasError() { return !status; }
    public String getErrorMessage() { return error_message; }

    /*public void runAgentForever_nonBlocking() {
        final Agent a = agent; // the final is effectively redundant due to the nature of the Soar agent, but good practice for spawning a thread
        Thread t = new Thread(new Runnable() {
            public void run() { 
              a.RunSelfForever();
            };
        });
        t.start();
    }*/

    public void cleanGameInput() {
        cleanMarioWMEs();
        // Remove all IDs that have been created underneath the main root objects
        refreshedObjectHashes.clear();
        removeUnusedIds();
    }
    
    public void resetAgent() {
        //cleanInputWMEs();
        cleanGameInput();
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

    private void cleanIDChildren(Identifier id) {
        ArrayList<WMElement> deleteList = new ArrayList<WMElement>(6);
        for (int i=0, iLim=id.GetNumberChildren(); i<iLim; ++i) {
            deleteList.add(id.GetChild(i));
        }
        for (WMElement wme : deleteList) {
            wme.DestroyWME();
        }
    }

    /*private void cleanAllBrickWMEs() {
        ArrayList<WMElement> deleteList = new ArrayList<WMElement>(11);
        for (int i=0, iLim=bricksRoot.GetNumberChildren(); i<iLim; ++i) {
            deleteList.add(bricksRoot.GetChild(i));
        }
        for (WMElement wme : deleteList) {
            wme.DestroyWME();
        }
    }*/

    private void addBrickIDAugs(Identifier brickId, Brick brick) {
        Mario mario = engine.getMapManager().getMario();
        brickId.CreateStringWME("type", brick.getType());
        brickId.CreateStringWME("is-breakable", (brick.isBreakable() ? "true" : "false"));
        brickId.CreateIntWME("x-absolute", (int)brick.getX());
        brickId.CreateIntWME("y-absolute", (int)brick.getY());
        brickId.CreateIntWME("x-relative", (int)mario.getRelativeX(brick.getX()));
        brickId.CreateIntWME("y-relative", (int)mario.getRelativeY(brick.getY()));

    }

    private void addEnemyIDAugs(Identifier enemyId, Enemy enemy) {
        Mario mario = engine.getMapManager().getMario();

        String type = "goomba";
        if (enemy instanceof KoopaTroopa)
            type = "koopatroopa";

        enemyId.CreateStringWME("type", type);
        enemyId.CreateIntWME("x-absolute", (int)enemy.getX());
        enemyId.CreateIntWME("y-absolute", (int)enemy.getY());
        enemyId.CreateIntWME("x-relative", (int)mario.getRelativeX(enemy.getX()));
        enemyId.CreateIntWME("y-relative", (int)mario.getRelativeY(enemy.getY()));
        enemyId.CreateFloatWME("x-speed", enemy.getVelX());
        enemyId.CreateFloatWME("y-speed", enemy.isFalling() ? -enemy.getVelYAbs() : enemy.getVelYAbs());
    }

    private String getPowerupType(BoostItem boost) {
        String type = null;
        if (boost instanceof SuperMushroom)
            type = "super-mushroom";
        else if (boost instanceof FireFlower)
            type = "fire-flower";
        else if (boost instanceof OneUpMushroom)
            type = "one-up-mushroom";
        
        return type;
    }

    private void addBoostIDAugs(Identifier boostId, BoostItem boost, String boostType) {
        Mario mario = engine.getMapManager().getMario();

        boostId.CreateStringWME("type", boostType);
        boostId.CreateIntWME("x-absolute", (int)boost.getX());
        boostId.CreateIntWME("y-absolute", (int)boost.getY());
        boostId.CreateIntWME("x-relative", (int)mario.getRelativeX(boost.getX()));
        boostId.CreateIntWME("y-relative", (int)mario.getRelativeY(boost.getY()));
        boostId.CreateFloatWME("x-speed", boost.getVelX());
        boostId.CreateFloatWME("y-speed", boost.isFalling() ? -boost.getVelYAbs() : boost.getVelYAbs());
    }

    private void addEndFlagIDAugs(Identifier flagId, EndFlag endFlag) {
        Mario mario = engine.getMapManager().getMario();

        flagId.CreateIntWME("x-absolute", (int)endFlag.getX());
        flagId.CreateIntWME("y-absolute", (int)endFlag.getY());
        flagId.CreateIntWME("x-relative", (int)mario.getRelativeX(endFlag.getX()));
        flagId.CreateIntWME("y-relative", (int)mario.getRelativeY(endFlag.getY()));
        //flagId.CreateIntWME("height", (int)endFlag.getBounds().getHeight());
    }

    private void cleanMarioWMEs() {
        ArrayList<WMElement> deleteList = new ArrayList<WMElement>(11);
        for (int i=0, iLim=marioHud.GetNumberChildren(); i<iLim; ++i) {
            deleteList.add(marioHud.GetChild(i));
        }
        for (int i=0, iLim=marioBody.GetNumberChildren(); i<iLim; ++i) {
            deleteList.add(marioBody.GetChild(i));
        }
        for (WMElement wme : deleteList) {
            wme.DestroyWME();
        }
    }

    public void updateInput_mario(Mario mario, double remainingTime) {
        // First clear the old values
        cleanMarioWMEs();

        // Add HUD values
        marioHud.CreateIntWME("time", (int)remainingTime);
        marioHud.CreateIntWME("points", mario.getPoints());
        marioHud.CreateIntWME("lives", mario.getRemainingLives());

        // Add Body values
        marioBody.CreateIntWME("x-absolute", (int)mario.getX());
        marioBody.CreateIntWME("y-absolute", (int)mario.getY());
        marioBody.CreateFloatWME("x-speed", mario.getVelX());
        marioBody.CreateFloatWME("y-speed", mario.getVelYAbs() * (mario.isFalling() ? 1.0 : -1.0));
        marioBody.CreateStringWME("is-super", (mario.isSuper() ? "true" : "false"));
        marioBody.CreateStringWME("is-fire", (mario.getMarioForm().isFire() ? "true" : "false"));
        marioBody.CreateIntWME("height", mario.getDimension().height);
        marioBody.CreateIntWME("width", mario.getDimension().width);
    }

    public void updateInput_touching(ArrayList<GameObject> marioCollidersTop, ArrayList<GameObject> marioCollidersBottom,
            ArrayList<GameObject> marioCollidersLeft, ArrayList<GameObject> marioCollidersRight) {
        addInput_touching_named("touching-top", marioCollidersTop);
        addInput_touching_named("touching-bottom", marioCollidersBottom);
        addInput_touching_named("touching-left", marioCollidersLeft);
        addInput_touching_named("touching-right", marioCollidersRight);
    }

    private void addInput_touching_named(String attrName, ArrayList<GameObject> colliderList) {
        // Add each touched object
        for (GameObject obj : colliderList) {
            Identifier id = objId_map.get(obj.hashCode());
            if (id != null) {
                marioBody.CreateSharedIdWME(attrName, id);
            }
        }
    }

    /**
     * Get the Identifier object needed to refresh input for the game object represented by the given hash.
     * If the object is already on the input-link, returns that Identifier after cleaning it of children WMEs.
     * Else, make a new Identifier, attached using the given root and attr name, and return that new Identifier.
     * @param obj_hash The hash of the game object that is to be added to the Soar agent's input-link
     * @param root The existing Identifier that the new Identifier should be added under, should it need to be added
     * @param attr The attribute name to use when adding the new Identifier, should it need to be added
     * @return
     */
    private Identifier getOrMakeCleanInputObjId(Integer obj_hash, Identifier root, String attr) {
        Identifier retvalId;

        if (!objId_map.containsKey(obj_hash)) {
            // It isn't on the input-link already. Create it.
            retvalId = root.CreateIdWME(attr);
            objId_map.put(obj_hash, retvalId);
        }
        else {
            // It is on the input-link already. Clean it for an update.
            retvalId = objId_map.get(obj_hash);
            cleanIDChildren(retvalId);
        }

        // Record that this input object was used this cycle
        refreshedObjectHashes.add(obj_hash);

        return retvalId;
    }

    public void updateInput_enemies(ArrayList<Enemy> enemyList) {
        if (enemyList.size() == 0)
            return;
            
        // Reference mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        // Add each enemy
        for (Enemy enemy : enemyList) {
            if (!map.isWithinCamera(enemy))
                continue;

            Integer hash = enemy.hashCode();
            // Get the existing object ID or create it if it doesn't yet exist
            Identifier enemyId = getOrMakeCleanInputObjId(hash, enemiesRoot, "enemy");
            // Add the object augmentations
            addEnemyIDAugs(enemyId, enemy);
        }
    }

    /**
     * Use the given list of Brick objects as input this cycle.
     * If any of the given bricks are already on the input-link, update them.
     * @param brickList The list of Bricks to use for the input-link this cycle
     */
    public void updateInput_bricks(ArrayList<Brick> brickList) {
        // Reference mario for relative coordinates
        //Mario mario = engine.getMapManager().getMario();
        Map map = engine.getMapManager().getMap();

        // Add each brick
        for (Brick b : brickList) {
            if (!map.isWithinCamera(b))
                continue;

            /*int relX = Math.abs((int)mario.getRelativeX(b.getX()));
            int relY = Math.abs((int)mario.getRelativeY(b.getY()));
            // Skip bricks that are farther than 4 blocks away
            if (relX > 48*4 || relY > 48*4)
                continue;*/

            Integer hash = b.hashCode();
            // Get the existing object ID or create it if it doesn't yet exist
            Identifier brickId = getOrMakeCleanInputObjId(hash, bricksRoot, "brick");
            // Add the object augmentations
            addBrickIDAugs(brickId, b);
        }
    }

    public void updateInput_powerups(ArrayList<BoostItem> pupsList) {
        if (pupsList.size() == 0)
            return;

        // Reference Mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        // Add each power-up
        for (BoostItem pup : pupsList) {
            if (!map.isWithinCamera(pup))
                continue;

            String pupType = getPowerupType(pup);
            // The returned String will be null if the BoostItem is not one of the valid classes
            if (pupType == null)
                continue;
            
            Integer hash = pup.hashCode();
            // Get the existing object ID or create it if it doesn't yet exist
            Identifier pupId = getOrMakeCleanInputObjId(hash, specialRoot, "power-up");
            // Add the object augmentations
            addBoostIDAugs(pupId, pup, pupType);
        }
    }

    public void updateInput_endFlag(EndFlag flagObj) {
        // Reference mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        if (!map.isWithinCamera(flagObj))
            return;

        Integer hash = flagObj.hashCode();
        // Get the existing object ID or create it if it doesn't yet exist
        Identifier flagId = getOrMakeCleanInputObjId(hash, specialRoot, "end-flag");
        // Add the object augmentations
        addEndFlagIDAugs(flagId, flagObj);
    }

    public void removeUnusedIds() {
        ArrayList<Integer> deleteList = new ArrayList<Integer>(30);
        for (Integer hash : objId_map.keySet()) {
            if (!refreshedObjectHashes.contains(hash)) {
                deleteList.add(hash);
            }
        }
        for (Integer hash : deleteList) {
            // First remove the ID from the input-link
            objId_map.get(hash).DestroyWME();
            // Then remove the record of the ID
            objId_map.remove(hash);
        }
        // Clean the refresh list
        refreshedObjectHashes.clear();
        // Refresh the agent's I/O
        agent.Commit();
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
