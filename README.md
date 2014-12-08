MonkeyMusicChallenge-Team-MusicMiners
=====================================

The repository for the agent used in the Monkey Music Challenge: http://www.monkeymusicchallenge.com/


Agent:
-----------

The main idea behind the agents AI is the following two steps:
	1. Evaluate the neighbourhoods, of size 0 <= k <= #Slots left in inventory, around all items in the map in order to find the best neighbourhood. Details about this is found in "Heuristic.pdf".
	2. Given the best neighbourhood and return user, compute the Travelling Salesman Problem where one visist all nodes in the neighbourhood and return to the return user. The agent should then move according to the solution of the stated TSP.


TODO: 

Tackling

Doors

Tunnels

Bananas

Traps