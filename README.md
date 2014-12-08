# MonkeyMusicChallenge-Team-MusicMiners

This is the repository for the agent used in the Monkey Music Challenge by our team "MusicMiners" consisting of Sebastian Ånerud and Roland Hellström Keyte.

The homepage of the challenge can be found at:
http://www.monkeymusicchallenge.com/

The GitHub for the challenge can be found at: https://github.com/monkey-music-challenge/

## Building and running the project

Open up a terminal and type:

git clone https://github.com/<username>/MonkeyMusicChallenge-Team-MusicMiners.git
cd MonkeyMusicChallenge-Team-MusicMiners
mvn package
java -jar target/warmup.jar <your-team-name> <your-api-key> <game-id>

## Disclaimer

The code was written under a tight time schedule and therefore the code might have a bad structure. The focus of this project was not to write beautiful code but to write an intelligent and efficient AI that runs as fast as possible.

An example of less beatiful code in this project is the accessing of public fields instead of using getters and setters. This was done purely for convenience and is not a habit.


## Agent

The main idea behind the agent's AI is:

1. Evaluate the neighbourhoods, of size 0 <= k <= #Slots left in inventory, around all items in the map in order to find the best neighbourhood and return user. Details about this is found in "Heuristic.pdf".

2. Given the best neighbourhood and return user, solve the Travelling Salesman Problem where one visist all nodes in the neighbourhood and return to the return user. The agent should then move according to the solution of the stated TSP.

In order to achieve this all pairwise distances between items, users and the agent are computed as needed with the A* algorithm and stored in a hash map for easy accessing. Storing the shortest paths in a hash map will also prevent the agent from recomputing them when it doesn't have to be done.


### Other key features of the agent

####Tackling

“Anger, fear, aggression; the dark side of the Force are they. Easily they flow, quick to join you in a fight. If once you start down the dark path, forever will it dominate your destiny, consume you it will, as it did Obi-Wan's apprentice." -Yoda

Basically, tackling is the first step down the path to the dark side and should not be abused in our oponion, instead it should be used wisely! So the agent does not tackle unless his inventory is empty AND he is between his closest user and the enemy AND the enemy is really close.


####Doors

The agent does not handle doors yet! However, this can easily be done by adding an extra dimension in the state space that take on the values 0 or 1 (0/1 for the mode of the lever). Then A* will take care of that automatically!


####Tunnels

The tunnels are connected in the A* algorithm when computed the shortest path between two positions in the map.


####Bananas

For all bananas _b_ on the map, check if taking a detour around banana _b_ will decrease the total length of your plan (eating the banana will make the agent move twice per turn). If there are bananas that shortens the plan, take a detour around the currently closest banana _b_* that shortens the plan. The agent will use a banana if there is a banana in the inventory and the agent doesn't already have the speedy buff.


####Traps

In the agents perspective the traps are just as a song, album or playlist but worth 1.9 points. This means that an agent prefer picking up a trap over a song but an album over a trap. The agent's inventory is also limited to 1 trap. If the agent at any point happens to have a trap in the inventory AND (is in a tight space OR beside a user) the agent will place the trap.

## Contact

Any questions are welcome to sebastian.anerud@gmail.com. We are happy to discuss more details about our agent and the thoughts behind our solutions to the different problems!