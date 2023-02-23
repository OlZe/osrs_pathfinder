package wiki.runescape.oldschool.pathfinder.logic;

public record DirectionalBlockers(
        boolean northBlocked,
        boolean eastBlocked,
        boolean southBlocked,
        boolean westBlocked) {
}
