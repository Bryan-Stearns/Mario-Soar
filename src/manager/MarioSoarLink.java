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

        // Default to watch-0, because the debugger printout can have a hard time keeping up with the game speed
        agent.ExecuteCommandLine("watch 0");

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

    /**
     * Destroys all child WMEs that are attached under the given Identifier
     * @param id The Identifier to clean
     */
    private void cleanAllIdChildren(Identifier id) {
        ArrayList<WMElement> deleteList = new ArrayList<WMElement>(6);
        for (int i=0, iLim=id.GetNumberChildren(); i<iLim; ++i) {
            deleteList.add(id.GetChild(i));
        }
        for (WMElement wme : deleteList) {
            wme.DestroyWME();
        }
    }

    /**
     * Destroys any WME children of the given Identifier if they contain the given attribute
     * @param id The Identifier to clean
     * @param attr The attribute to clean from the given identifier
     * @param count How many WMES with the given attribute to clean. (Give a negative value for no limit.)
     * @return Whetner any WME children were cleaned
     */
    private boolean cleanIdAttrs(Identifier id, String attr, int count) {
        boolean retval = false;
        ArrayList<WMElement> deleteList = new ArrayList<WMElement>(11);
        for (int i=0, iLim=id.GetNumberChildren(); i<iLim; ++i) {
            WMElement child = id.GetChild(i);
            if (child.GetAttribute().equals(attr)) {
                deleteList.add(child);
                retval = true;
                if (--count == 0)
                    break;
            }
        }
        for (WMElement wme : deleteList) {
            wme.DestroyWME();
        }
        return retval;
    }

    /*private boolean IdContainsWMEChild(Identifier id, String attr, String val) {
        for (int i=0, iLim=id.GetNumberChildren(); i<iLim; ++i) {
            WMElement wme = id.GetChild(i);
            if (wme.GetAttribute().equals(attr) && wme.GetValueAsString().equals(val))
                return true;
        }
        return false;
    }*/

    private void refreshEnemyIDAugs(Identifier enemyId, Enemy enemy) {
        Mario mario = engine.getMapManager().getMario();
        // Since only the type is unchanging, just clean it all and remake it. Saves processing.
        cleanAllIdChildren(enemyId);

        String type = "goomba";
        if (enemy instanceof KoopaTroopa)
            type = "koopatroopa";

        enemyId.CreateStringWME("type", type);
        enemyId.CreateIntWME("x", (int)enemy.getX());
        enemyId.CreateIntWME("y", (int)enemy.getY());
        enemyId.CreateIntWME("x-distance", Math.abs((int)mario.getRelativeX(enemy.getX())));
        enemyId.CreateIntWME("y-distance", Math.abs((int)mario.getRelativeY(enemy.getY())));
        enemyId.CreateFloatWME("x-speed", enemy.getVelX());
        enemyId.CreateFloatWME("y-speed", enemy.isFalling() ? -enemy.getVelYAbs() : enemy.getVelYAbs());
        enemyId.CreateFloatWME("distance", mario.getDistance(enemy.getX(), enemy.getY()));
    }

    private void refreshBrickIDAugs(Identifier brickId, Brick brick) {
        Mario mario = engine.getMapManager().getMario();
        if (brickId.GetNumberChildren() == 0) {
            // If this is a blank slate, make the unchanging augmentations
            brickId.CreateStringWME("type", brick.getType());
            brickId.CreateStringWME("is-breakable", (brick.isBreakable() ? "true" : "false"));
            brickId.CreateIntWME("x", (int)brick.getX());
            brickId.CreateIntWME("y", (int)brick.getY());
        }
        else {
            // If this is an update to an existing object, remove the old volatile augmentations
            cleanIdAttrs(brickId, "x-distance", 1);
            cleanIdAttrs(brickId, "y-distance", 1);
            cleanIdAttrs(brickId, "distance", 1);
        }
        // Add new content for the volatile augmentations
        brickId.CreateIntWME("x-distance", Math.abs((int)mario.getRelativeX(brick.getX())));
        brickId.CreateIntWME("y-distance", Math.abs((int)mario.getRelativeY(brick.getY())));
        brickId.CreateFloatWME("distance", mario.getDistance(brick.getX(), brick.getY()));
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

    private void refreshBoostIDAugs(Identifier boostId, BoostItem boost, String boostType) {
        Mario mario = engine.getMapManager().getMario();
        // Since only the type is unchanging, just clean it all and remake it. Saves processing.
        cleanAllIdChildren(boostId);

        boostId.CreateStringWME("type", boostType);
        boostId.CreateIntWME("x", (int)boost.getX());
        boostId.CreateIntWME("y", (int)boost.getY());
        boostId.CreateIntWME("x-distance", Math.abs((int)mario.getRelativeX(boost.getX())));
        boostId.CreateIntWME("y-distance", Math.abs((int)mario.getRelativeY(boost.getY())));
        boostId.CreateFloatWME("x-speed", boost.getVelX());
        boostId.CreateFloatWME("y-speed", boost.isFalling() ? -boost.getVelYAbs() : boost.getVelYAbs());
        boostId.CreateFloatWME("distance", mario.getDistance(boost.getX(), boost.getY()));
    }

    private void refreshEndFlagIDAugs(Identifier flagId, EndFlag endFlag) {
        Mario mario = engine.getMapManager().getMario();
        // Just clean it all and remake it. Saves processing when the flag is moving.
        cleanAllIdChildren(flagId);

        flagId.CreateIntWME("x", (int)endFlag.getX());
        flagId.CreateIntWME("y", (int)endFlag.getY());
        flagId.CreateIntWME("x-distance", Math.abs((int)mario.getRelativeX(endFlag.getX())));
        flagId.CreateIntWME("y-distance", Math.abs((int)mario.getRelativeY(endFlag.getY())));
        flagId.CreateFloatWME("distance", mario.getDistance(endFlag.getX(), endFlag.getY()));
        //flagId.CreateIntWME("height", (int)endFlag.getBounds().getHeight());
    }

    /**
     * Get the Identifier object needed to refresh input for the game object represented by the given hash.
     * If the object is already on the input-link, returns that Identifier.
     * Else, make a new Identifier, attached using the given root and attr name, and return that new Identifier.
     * @param obj_hash The hash of the game object that is to be added to the Soar agent's input-link
     * @param root The existing Identifier that the new Identifier should be added under, should it need to be added
     * @param attr The attribute name to use when adding the new Identifier, should it need to be added
     * @return
     */
    private Identifier getOrMakeInputObjId(Integer obj_hash, Identifier root, String attr) {
        Identifier retvalId;

        if (!objId_map.containsKey(obj_hash)) {
            // It isn't on the input-link already. Create it.
            retvalId = root.CreateIdWME(attr);
            objId_map.put(obj_hash, retvalId);
        }
        else {
            // It is on the input-link already.
            retvalId = objId_map.get(obj_hash);
            //cleanAllIdChildren(retvalId);
        }

        // Record that this input object was used this cycle
        refreshedObjectHashes.add(obj_hash);

        return retvalId;
    }

    private Identifier getInputObjId(Integer obj_hash) {

        if (!objId_map.containsKey(obj_hash)) {
            // It isn't on the input-link already.
            return null;
        }
        else {
            // It is on the input-link already. Return it.
            return objId_map.get(obj_hash);
        }
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
        marioBody.CreateIntWME("x", (int)mario.getX());
        marioBody.CreateIntWME("y", (int)mario.getY());
        marioBody.CreateFloatWME("x-speed", mario.getVelX());
        marioBody.CreateFloatWME("y-speed", mario.getVelYAbs() * (mario.isFalling() ? 1.0 : -1.0));
        marioBody.CreateStringWME("is-super", (mario.isSuper() ? "true" : "false"));
        marioBody.CreateStringWME("is-fire", (mario.getMarioForm().isFire() ? "true" : "false"));
        marioBody.CreateIntWME("height", mario.getDimension().height);
        marioBody.CreateIntWME("width", mario.getDimension().width);
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
            Identifier enemyId = getOrMakeInputObjId(hash, enemiesRoot, "enemy");
            // Add the object augmentations
            refreshEnemyIDAugs(enemyId, enemy);
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

        boolean[] inCameraMask = new boolean[brickList.size()]; // Reminder: Java auto-initializes all values to false

        // Add each brick if it is in the camera view
        for (int i=0, iLim=brickList.size(); i<iLim; ++i) {
            Brick b = brickList.get(i);

            if (!map.isWithinCamera(b))
                continue;

            // Mark for later that this brick is within the camera view
            inCameraMask[i] = true;

            /*int relX = Math.abs((int)mario.getRelativeX(b.getX()));
            int relY = Math.abs((int)mario.getRelativeY(b.getY()));
            // Skip bricks that are farther than 4 blocks away
            if (relX > 48*4 || relY > 48*4)
                continue;*/

            // Get the existing object ID or create it if it doesn't yet exist
            Identifier brickId = getOrMakeInputObjId(b.hashCode(), bricksRoot, "brick");
            // Add the object augmentations
            refreshBrickIDAugs(brickId, b);

            // Clear old touch augmentations
            cleanIdAttrs(brickId, "touching-left", -1);
            cleanIdAttrs(brickId, "touching-right", -1);
            cleanIdAttrs(brickId, "touching-top", -1);
            cleanIdAttrs(brickId, "touching-bottom", -1);
        }

        // Go through the list again and mark which bricks are touching
        for (int i=0, iLim=brickList.size()-1; i<iLim; ++i) {
            if (!inCameraMask[i]) {
                continue;
            }
            Brick b1 = brickList.get(i);
            Identifier b1Id = getInputObjId(b1.hashCode());

            // Ignore pipes for touch checks
            if (b1.getType().equals("pipe")) {
                continue;
            }

            // Compare with all later bricks in the map view for adjacency
            for (int j=i+1, jLim=brickList.size(); j<jLim; ++j) {
                if (!inCameraMask[j]) {
                    continue;
                }
                Brick b2 = brickList.get(j);
                
                // Ignore pipes for touch checks
                if (b2.getType().equals("pipe")) {
                    continue;
                }

                Identifier b2Id = getInputObjId(b2.hashCode());

                // Test if they have the same y
                if (b1.getY() == b2.getY()) {
                    int xDiff = (int)(b1.getX() - b2.getX());
                    // If they are only brick.dimension.width=48 apart in x, they are touching
                    if (xDiff <= 48 && xDiff > 0) {
                        b1Id.CreateSharedIdWME("touching-left", b2Id);
                        b2Id.CreateSharedIdWME("touching-right", b1Id);
                    }
                    else if (xDiff >= -48 && xDiff < 0) {
                        b1Id.CreateSharedIdWME("touching-right", b2Id);
                        b2Id.CreateSharedIdWME("touching-left", b1Id);
                    }
                }
                // Test if they have the same x
                else if (b1.getX() == b2.getX()) {
                    int yDiff = (int)(b1.getY() - b2.getY());
                    // If they are only brick.dimension.height=48 apart in y, they are touching
                    if (yDiff <= 48 && yDiff > 0) {
                        b1Id.CreateSharedIdWME("touching-top", b2Id);
                        b2Id.CreateSharedIdWME("touching-bottom", b1Id);
                    }
                    else if (yDiff >= -48 && yDiff < 0) {
                        b1Id.CreateSharedIdWME("touching-bottom", b2Id);
                        b2Id.CreateSharedIdWME("touching-top", b1Id);
                    }
                }
            } // END for (int j=i+1, jLim=brickList.size(); j<jLim; ++j)
        } // END for (int i=0, iLim=brickList.size()-1; i<iLim; ++i)
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
            
            // Get the existing object ID or create it if it doesn't yet exist
            Identifier pupId = getOrMakeInputObjId(pup.hashCode(), specialRoot, "power-up");
            // Add the object augmentations
            refreshBoostIDAugs(pupId, pup, pupType);
        }
    }

    public void updateInput_endFlag(EndFlag flagObj) {
        // Reference mario for relative coordinates
        Map map = engine.getMapManager().getMap();

        if (!map.isWithinCamera(flagObj))
            return;

        // Get the existing object ID or create it if it doesn't yet exist
        Identifier flagId = getOrMakeInputObjId(flagObj.hashCode(), specialRoot, "end-flag");
        // Add the object augmentations
        refreshEndFlagIDAugs(flagId, flagObj);
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

    public void addInput_touching(ArrayList<GameObject> marioCollidersTop, ArrayList<GameObject> marioCollidersBottom,
            ArrayList<GameObject> marioCollidersLeft, ArrayList<GameObject> marioCollidersRight) {
        addInput_touching_named("touching-top", marioCollidersTop);
        addInput_touching_named("touching-bottom", marioCollidersBottom);
        addInput_touching_named("touching-left", marioCollidersLeft);
        addInput_touching_named("touching-right", marioCollidersRight);
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
