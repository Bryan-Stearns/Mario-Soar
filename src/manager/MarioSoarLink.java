package manager;

import java.util.ArrayList;

import com.optum.ect.soar.SoarLinkAbstract;
import com.optum.ect.soar.SoarMemObj;
import com.optum.ect.soar.SoarValueType;

import model.brick.Brick;
import model.hero.Mario;
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
        marioBody.add_aug("y-speed", mario.getVelYAbs());
        marioBody.add_aug("is-super", (mario.isSuper() ? "true" : "false"));
        marioBody.add_aug("is-fire", (mario.getMarioForm().isFire() ? "true" : "false"));
        marioBody.add_aug("height", mario.getDimension().height);
        marioBody.add_aug("width", mario.getDimension().width);

        // Add this tree to the input link
        addInputWMEs(ilTree);
    }

    public void addInput_bricks(ArrayList<Brick> brickList) {
        // Reference mario for relative coordinates
        Mario mario = engine.getMapManager().getMario();

        // Create main structure
        SoarMemObj ilTree = new SoarMemObj("root", SoarValueType.OBJECT),
                bricksObj = new SoarMemObj("bricks", SoarValueType.OBJECT);
        ilTree.add_aug("bricks", bricksObj);

        // Add each brick
        for (Brick b : brickList) {
            int relX = (int)mario.getRelativeX(b.getX());
            int relY = (int)mario.getRelativeY(b.getY());
            // Skip bricks that are farther than 4 blocks away
            if (relX > 48*4 || relY > 48*4)
                continue;

            SoarMemObj brick = new SoarMemObj("brick", SoarValueType.OBJECT);
            brick.add_aug("type", b.getType());
            brick.add_aug("is-breakable", (b.isBreakable() ? "true" : "false"));
            brick.add_aug("x-absolute", (int)b.getX());
            brick.add_aug("y-absolute", (int)b.getY());
            brick.add_aug("x-relative", relX);
            brick.add_aug("y-relative", relY);

            // Attach this brick to the tree
            bricksObj.add_aug("brick", brick);
        }

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
        
        for (int i=0, iLim=agent.GetNumberCommands(); i<iLim; ++i) {
            Identifier commandId = agent.GetCommand(i);

			if (commandId.GetParameterValue("status") != null) {
				//System.out.println("Status already processed.");
				continue;
			}

            String commandAttr = commandId.GetAttribute();
            
            if (commandAttr.equals("message")) {
                String message = commandId.GetParameterValue("value");
                output_messageGiven = true;
                engine.setAgentMessage(message);
                continue;
            }
            else if (commandAttr.equals("key-press")) {
                String keyCode = commandId.GetParameterValue("value");

                if (keyCode.equals("A")) {
                    output_keyPressed_A = true;
                    commandId.AddStatusComplete();
                    continue;
                }
                else if (keyCode.equals("B")) {
                    output_keyPressed_B = true;
                    commandId.AddStatusComplete();
                    continue;
                }
                else if (keyCode.equals("left")) {
                    output_keyPressed_left = true;
                    commandId.AddStatusComplete();
                    continue;
                }
                else if (keyCode.equals("right")) {
                    output_keyPressed_right = true;
                    commandId.AddStatusComplete();
                    continue;
                }
                else {
                    commandId.AddStatusError();
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
        if (messageGiven && !output_messageGiven) {
            // Clear message text display if no more message
            //engine.setAgentMessage("");
            messageGiven = false;
        }

        // LEFT KEY
        if (!keyPressed_left && output_keyPressed_left) {
            // Press left key
            keyPressed_left = true;
            engine.receiveInput(ButtonAction.M_LEFT);
        }
        if (keyPressed_left && !output_keyPressed_left) {
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
        if (keyPressed_right && !output_keyPressed_right) {
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
        if (keyPressed_A && !output_keyPressed_A) {
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
        if (keyPressed_B && !output_keyPressed_B) {
            // Release B key
            keyPressed_B = false;
        }
    }
    
}
