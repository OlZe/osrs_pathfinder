# OSRS Pathfinder Prototype Project

This is a webapplication featuring a map of the game Old School Runescape with pathfinding features.

The [frontend](https://github.com/OlZe/osrs_pathfinder_frontend) is served as a client-side rendered website. It queries the backend for pathfinding requests and displays the results in the map.

The backend reads previously dumped OSRS map data to build a graph and thus perform on-demand pathfinding requests on.

The movement data of the map is gathered in [this project](https://github.com/OlZe/osrs_pathfinder_movement_dumper). Transport and Teleport data is partially prepared by hand and partially copied by [Skretzo's shortest path plugin](https://github.com/Skretzo/shortest-path).

# Usage

This is a Spring Boot application.

Once started, go to http://localhost:8080/frontend/index.html
