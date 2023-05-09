package wiki.runescape.oldschool.pathfinder.data_deserialization;

import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

public record TeleportJson(Coordinate to,
                           String title,
                           byte duration,
                           boolean canTeleportUpTo30Wildy) {
}
