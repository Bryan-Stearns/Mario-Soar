# Super Mario Bros.

Classic Super Mario Bros. game implemented with Java, modified to interface with Soar cognitive architecture agents for the sake of testing and programmer training. 
Original source code for the java game at https://github.com/ahmetcandiroglu/Super-Mario-Bros. 
Note that this is not as full-featured as the original Super Mario Bros. game. It is only a simple imitation that allows easy sandbox agent development.

## Using with Soar

* You must first have Soar installed on your machine, with the SOAR_HOME environment variable set appropriately. (See the Soar website for details on installation.)
* Run the .jar file with no command line arguments to play the game yourself.
* Run the .jar file with a path to a .soar agent file as a first argument to have that Soar agent play the game.
    * (See the docs/agent-io-spec.txt file to see the input/output structures that your Soar agent can use.)
* Run the .jar file with the "--open-debugger" flag as a second argument to automatically open a Soar Java Debugger with your Soar agent.
    * (Press F1 while the game is running to toggle debug mode, which shows object coordinates on the screen (you can compare these with agent input structures) and allows human control intervention.)

## Game code modifications by Bryan Stearns

* Changed character collision mechanics to be closer to the original game
* Made it so enemies that kill Mario are not immediately killed and removed
* Added small jump to Mario when he squishes an enemy
* Allowed Mario+camera to move back to the left
* Fixed collision bugs with enemies and bricks
* Improved efficiency by only drawing bricks and enemies that are within the camera view
* Added Mario jump and fall animation when Mario dies
* Made automatic sequence of Mario-dies -> YouDied screen -> Main menu
* Adjusted Mario jump height
* Made game restart after winning
* Removed fireballs when they leave the screen
* Fixed so Mario can keep moving left/right if jumping clears an obstacle that had been previously hit while moving left/right
* Added ability to hold down Jump to jump higher
* Fixed exception when Mario touches a Coin from a ? block.
* Added invulnerability time while camera is shaking (Mario is shrinking from damage)
* Fixed bug where Koopa can cancel out its reverse-VelX when colliding with two stacked bricks at once
* Fixed Mario walk animation speed
* Added walking animation to Goombas
* Made Mario disappear into castle at level end
* Turned off collision detection while Mario shrinks
* Made Mario y-value adjust when he shrinks
* Fixed bug where camera.shaking would never turn false once made true
* Made enemies reverse x-direction when hitting Mario and making him shrink
* Added debug command F1 to show object coordinates and allow control override over Soar agent
* Added ability to run when holding down the Fire button
* Tweaked walking/running speeds

## In-game Screens

### Start Screen
![start screen](https://raw.githubusercontent.com/ahmetcandiroglu/1G.Super-Mario-Bros/master/docs/Screenshots/Start%20screen.png)

### Inside Game
![in game screen](https://raw.githubusercontent.com/ahmetcandiroglu/1G.Super-Mario-Bros/master/docs/Screenshots/In%20game%20screen.png)

### Pause Screen
![pause screen](https://raw.githubusercontent.com/ahmetcandiroglu/1G.Super-Mario-Bros/master/docs/Screenshots/Pause%20screen.png)
