package com.imperiled.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Event;

/**
 * A class for the events on a map. An instance
 * of this class should never be created directly.
 * It should be created by the class FileParser which
 * reads in all the relevant events for the map.
 * 
 * Requires properties for every MapEvent:
 * name
 * action
 * target
 * 
 * Optional properties for every MapEvent:
 * repeatable
 * 
 * @author John Wikman
 * @version 2015.05.12
 */
public class MapEvent extends Event {
	private HashMap<String, String> props;
	
	/**
	 * Constructor for a MapEvent. This should only
	 * be called by the class FileParser.
	 */
	public MapEvent(HashMap<String, String> props) {
		this.props = props;
	}
	
	/**
	 * Returns the name of this event.
	 */
	public String getName() {
		return props.get("name");
	}
	
	/**
	 * The method that triggers the event. It does different
	 * things depending on what the action property is.
	 */
	public void action() {
		// Loads the action-type and sets the event as handled
		// unless repeatable is specified to be true.
		String act = props.get("action");
		String target = props.get("target");
		String repeatable = props.get("repeatable");
		if(isHandled()) {
			return;
		}
		if(repeatable == null || !repeatable.equalsIgnoreCase("true")) {
			handle();
		}
		
		/*
		 * Causes eventError if specified target is not
		 * loaded on to the active map.
		 */
		if(PropertyHandler.currentActors.get(target) == null) {
			eventError("Target is not loaded on to this map.", act, target);
		}
		
		/*
		 * This action-type does damage to a specific
		 * target. An amount specifies the amount of
		 * damage to deal to the target.
		 * 
		 * Causes eventError if "target" is not
		 * specified or is not loaded on map. Causes
		 * eventError if "amount" is not specified or
		 * if "amount" is not a number.
		 * 
		 * Required properties:
		 * amount
		 */
		if(act.equalsIgnoreCase("dodamage")) {
			String sAmount = props.get("amount");
			if(sAmount == null) {
				eventError("No damage amount specified.", act, target);
			}
			if(!sAmount.matches("^\\d+$")) {
				eventError("Amount is not a number.", act, target);
			}
			int amount = Integer.parseInt(sAmount);
			PropertyHandler.currentActors.get(target).takeDamage(amount);
			return;
		}
		
		/*
		 * This action-type changes the current map. It
		 * also specifies what coordinates the player
		 * will spawn on the new map and the direction
		 * the player will be facing.
		 * 
		 * Causes eventError if specified mapTarget
		 * does not exist, if specified coordinates
		 * are not integers or if the specified direction
		 * is not UP, DOWN, LEFT or RIGHT.
		 * 
		 * Required properties:
		 * mapTarget
		 * 
		 * Optional properties:
		 * xcor & ycor
		 * direction
		 */
		if(act.equalsIgnoreCase("changeMap")) {
			String mapTarget = props.get("mapTarget");
			String xcor = props.get("xcor");
			String ycor = props.get("ycor");
			String direction = props.get("direction");
			if(!mapExists(mapTarget)) {
				eventError("Target map does not exist.", act, mapTarget);
			}
			Imperiled game = PropertyHandler.currentGame; 
			game.map = mapTarget;
			game.startPos = null;
			if(xcor != null && ycor != null) {
				if(!xcor.matches("^\\d+$") || !ycor.matches("^\\d+$")) {
					eventError("Coordinates not integers.", act, mapTarget);
				}
				game.startPos = new Vector2();
				game.startPos.x = Integer.parseInt(xcor);
				game.startPos.y = Integer.parseInt(ycor);
			}
			if(direction == null) {
				//Do nothing
			} else if(direction.equalsIgnoreCase("UP")) {
				game.startDirection = Direction.UP;
			} else if(direction.equalsIgnoreCase("DOWN")) {
				game.startDirection = Direction.DOWN;
			} else if(direction.equalsIgnoreCase("LEFT")) {
				game.startDirection = Direction.LEFT;
			} else if(direction.equalsIgnoreCase("RIGHT")) {
				game.startDirection = Direction.RIGHT;
			} else {
				eventError("Specified direction not valid.", act, target);
			}
			//Get the difference in players health
			//to save it for the new map.
			game.playerHealth = PropertyHandler.currentActors.get("player").health;
			game.setScreen(new MainGameScreen(game));
			return;
		}
		
		/*
		 * This action-typ sets the health of a target
		 * to it's specified max health.
		 * 
		 * Only the three standard properties are
		 * required.
		 */
		if(act.equalsIgnoreCase("restoreHealth")) {
			Actor trgt = PropertyHandler.currentActors.get(target);
			trgt.health = trgt.maxHP;
			return;
		}
		
		/*
		 * Lets the player go to a winning screen, i.e a player has won
		 * the game. 
		 * 
		 * No required arguments nor any optional
		 */
		if(act.equalsIgnoreCase("winGame")){
			//player has finished the game
			Imperiled game = PropertyHandler.currentGame;
			game.setScreen(new WinScreen(game));
			return;
		}
		
		/*
		 * If the player enters this area, it checks if all
		 * the actors in the current map are inactive, and if they
		 * are , we remove the collisionObject specified in the event
		 * 
		 * Required argument:
		 * collisionObject ,target should be player
		 * 
		 */
		if(act.equalsIgnoreCase("blockUntilClear")){
			String collisionObjectName = props.get("collisionObject");
			//check that no enemy is still alive
			Iterator<Entry<String, Actor>> iterActor = PropertyHandler.currentActors.entrySet().iterator();
			boolean anyOneAlive = false;
			while(iterActor.hasNext()){
				Entry<String, Actor> nextActor = iterActor.next();
				if(nextActor.getValue().currentState != State.INACTIVE && !nextActor.getValue().name.equals("player")){
					System.out.println(nextActor.getValue().name);
					anyOneAlive = true;
					break;
				}
			}
			if(!anyOneAlive){
				//no one is alive, circle through all the collision objects until we find what
				//we are looking for and remove it
				Iterator<MapObject> iterCollision = PropertyHandler.collisionObjects.iterator();
				while(iterCollision.hasNext()){
					MapObject nextCollision = iterCollision.next();
					if(nextCollision.getName() != null && nextCollision.getName().equals(collisionObjectName)){
						iterCollision.remove();
					}
				}
			}
			return;
		}
	}
	
	/**
	 * Returns the target of this event.
	 * Returns null of there is no target.
	 */
	public String eventTarget() {
		return props.get("target");
	}
	
	/**
	 * Prints out a list of properties for an event.
	 * Used for debugging.
	 * 
	 * @return A String containing all the properties of the event.
	 */
	public String listOfProperties() {
		StringBuilder s = new StringBuilder();
		for(String key : props.keySet()) {
			s.append("\n" + key + ": " + props.get(key));
		}
		return s.toString();
	}
	
	/**
	 * Help method to check if a map exists.
	 * @return True if the map exists. Else false;
	 */
	private boolean mapExists(String mapName) {
		if(mapName == null) {
			return false;
		}
		return Gdx.files.internal("map/" + mapName + ".tmx").name().equals(mapName + ".tmx");
	}
	
	/**
	 * Help method that causes an eventError which
	 * prints an error message and exits the program.
	 * 
	 * @param description A description of the error.
	 * @param act What action was performed.
	 * @param causeVal The value that caused the error.
	 */
	private void eventError(String description, String act, String causeVal) {
		System.err.printf("Event error: %s%nAction: %s%nError cause value: %s.%n", description, act, causeVal);
		System.exit(1);
	}
}
