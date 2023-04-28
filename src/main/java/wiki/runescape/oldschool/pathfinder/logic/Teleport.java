package wiki.runescape.oldschool.pathfinder.logic;

public record Teleport(
        Coordinate destination,
        String title,
        byte duration,
        boolean canTeleportUpTo30Wildy) {

}
