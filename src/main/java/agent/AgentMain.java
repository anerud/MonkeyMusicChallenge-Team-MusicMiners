package agent;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

// Hi! Welcome to the Monkey Music Challenge Java starter kit!

public class AgentMain {

	private static String gameId;

	// You control your monkey by sending POST requests to the Monkey Music server
	private static final String GAME_URL = "http://competition.monkeymusicchallenge.com/game";

	public static void main(final String[] args) {

		// Don't forget to provide the right command line arguments
		if (args.length < 3) {
			if (args.length < 1) {
				System.out.println(" Missing argument: <your-team-name>");
			}
			if (args.length < 2) {
				System.out.println(" Missing argument: <your-api-key>");
			}
			if (args.length < 3) {
				try {
					BufferedReader br = new BufferedReader(new FileReader("gameId.txt"));
					gameId = br.readLine();
					br.close();
				} catch (FileNotFoundException e) {
					System.out.println(" Missing argument: <game-id>");
				} catch (IOException e) {
					System.out.println(" Missing argument: <game-id>");
				}
				
			}
			if(gameId == null) {
				System.out.println("Usage: java -jar target/monkey.jar <your-team-name> <your-api-key> <game-id>\n");
				System.exit(1);
			}
		}

		// You identify yourselves by your team name, your API key, and the current game ID
		final String teamName = args[0];
		final String apiKey = args[1];
		if(gameId == null){
			gameId = args[2];
		}

		// We've put the AI-code in a separate class
		final AgentNeighbourhoodHeuristic ai = new AgentNeighbourhoodHeuristic();

		// Allright, time to get started!

		// When we POST a command to the server, it always replies with the current game state
		final Map<String, Object> joinGameCommand = new HashMap<String, Object>();
		joinGameCommand.put("command", "join game");
		joinGameCommand.put("team", teamName);
		joinGameCommand.put("apiKey", apiKey);
		joinGameCommand.put("gameId", gameId);

		JSONObject currentGameState = postToServer(joinGameCommand);

		// The current game state tells you if the game is over
		while (!currentGameState.getBoolean("isGameOver")) {
			// The game is not over, time to make a move!
			System.out.println("");
			System.out.println("Remaining turns: " + currentGameState.getInt("remainingTurns"));

			// Use your AI to decide in which direction to move
			final Map<String, Object> nextCommand = ai.move(currentGameState);
			// Don't forget to include your credentials!
			nextCommand.put("team", teamName);
			nextCommand.put("apiKey", apiKey);
			nextCommand.put("gameId", gameId);

			// ...and send a new move command to the server
			long t2 = System.currentTimeMillis();
			currentGameState = postToServer(nextCommand);
			System.out.println("Server responded in: " + (System.currentTimeMillis() - t2));
			// After sending your command, you'll get the new game state back
			// and we go back up the loop to calculate our next move.

			System.out.println("");
			System.out.println("--------------------------------------------------------------");
		}

		// If the game is over, our server will tell you how you did
		System.out.println("\nGame over!");
		System.exit(0);
	}

	// Every time we POST a command to the server, we get the current game state back
	private static JSONObject postToServer(final Map<String, Object> command) {
		try {
			// In this starter kit, we use the Unirest library to send POST requests
			final HttpResponse<JsonNode> response = Unirest.post(GAME_URL)
					.header("Content-Type", "application/json")
					.body(new JSONObject(command).toString())
					.asJson();

			if (response.getCode() == 200) {
				return response.getBody().getObject();
			} else {
				if (response.getBody().getObject().has("message")) {
					System.out.println(response.getBody().getObject().getString("message"));
				}
				throw new RuntimeException("Server replied with status code " + response.getCode());
			}
		} catch (final Exception e) {
			// Hopefully, our server will always be able to handle your requests, but you never know...
			System.out.println(e.getMessage());
			System.exit(1);
			throw new AssertionError(); // unreachable
		}
	}

}
