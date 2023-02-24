# OSRS Pathfinder Prototype Project

This is a webserver serving a webapplication featuring a map of the game Old School Runescape with pathfinding features to find the fastest path for a given start and end position using in-game teleports, transports (such as ladders, agility shortcuts, ...) and plain walking.

The [frontend](https://github.com/OlZe/osrs_pathfinder_frontend), which is developed in a separate repository, is served as a client-side rendered website. It queries the webserver for pathfinding requests and displays the results in the map.

The webserver reads previously dumped OSRS map data to build a graph and perform on-demand pathfinding requests on. The map data, which includes all walkable tiles, transports and teleports, is gathered in [the Movement Dumper project](https://github.com/OlZe/osrs_pathfinder_movement_dumper).

# Usage

Run `git submodule update --remote --recursive` which can take several minutes

Run this project as a Spring Boot application.

Once started, go to http://localhost:8080/frontend/index.html
