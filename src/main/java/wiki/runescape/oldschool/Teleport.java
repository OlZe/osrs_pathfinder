package wiki.runescape.oldschool;

/**
 * Represents a teleport that can be used anywhere in the game.
 * Eg: Varrock Teleport
 */
public record Teleport(GraphVertex destination, String title, byte duration) {

}
