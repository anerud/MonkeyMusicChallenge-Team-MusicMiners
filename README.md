MonkeyMusicChallenge-Team-MusicMiners
=====================================

The repository for the agent used in the Monkey Music Challenge: http://www.monkeymusicchallenge.com/


Disclaimer
-----------
The code was written under a tight time schedule and therefore the code might have a bad structure. The focus of this project was not to write beautiful code but to write an intelligent and efficient AI that runs as fast as possible.

An example of less beatiful code in this project is the accessing of public fields instead of using getters and setters. This was done purely for convenience and is not a habit.


Agent
-----------
The main idea behind the agents AI is the following two steps:

1. Evaluate the neighbourhoods, of size 0 <= k <= #Slots left in inventory, around all items in the map in order to find the best neighbourhood. Details about this is found in "Heuristic.pdf".

2. Given the best neighbourhood and return user, compute the Travelling Salesman Problem where one visist all nodes in the neighbourhood and return to the return user. The agent should then move according to the solution of the stated TSP.


To be filled in: 

Tackling

Doors

Tunnels

Bananas

Traps