# Super Mario Bros.

Classic Super Mario Bros. game implemented with Java, modified to interface with Soar cognitive architecture agents for the sake of testing and programmer training. 
Original source code for the java game at https://github.com/ahmetcandiroglu/Super-Mario-Bros

## About game

You can visit [wikipedia page](https://en.wikipedia.org/wiki/Super_Mario_Bros.) or [Super Mario wiki page](https://www.mariowiki.com/Super_Mario_Bros.) for detailed information about the game.

## Built With
* [Java](https://www.java.com/)

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
* Made Mario y value adjust when he shrinks
* Fixed bug where camera.shaking would never turn false once made true
* Made enemies reverse x-direction when hitting Mario and making him shrink

## In-game Screens

### Start Screen
![start screen](https://raw.githubusercontent.com/ahmetcandiroglu/1G.Super-Mario-Bros/master/docs/Screenshots/Start%20screen.png)

### Inside Game
![in game screen](https://raw.githubusercontent.com/ahmetcandiroglu/1G.Super-Mario-Bros/master/docs/Screenshots/In%20game%20screen.png)

### Pause Screen
![pause screen](https://raw.githubusercontent.com/ahmetcandiroglu/1G.Super-Mario-Bros/master/docs/Screenshots/Pause%20screen.png)
