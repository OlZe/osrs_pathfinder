package cooksImplementation;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.Headers;
import com.google.gson.Gson;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.FileReader;
import java.io.BufferedReader;

/*
    EXAMPLE API CALLS

    Lumbridge, 1 Tile
        http://localhost:8128/path.json?from=3221,3219&to=3222,3219

    Lumbridge to Goblins
        http://localhost:8128/path.json?from=3221,3219&to=3246,3243

    Lumbridge to Varrock Square
        http://localhost:8128/path.json?from=3221,3219&to=3212,3430



 */




// this class runs the HttpServer and hands off requests to cooksImplementation.PathLogic
public class Pathfinder {
    static Gson gson = new Gson();
    public static void main(String[] args) throws Exception {
        System.out.println("Starting setup...");
        PathLogic.setup();
        System.out.println("Setup finished...");

        HttpServer server = HttpServer.create(new InetSocketAddress(8128), 0);
        server.createContext("/path.json", new MyHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    // this converts a query string to a map from keys to values
    static Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

    // this hands off requests to path.json to cooksImplementation.PathLogic.
    // much of this is copied from the interwebs
    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            String response = "[]";
            if (query != null) {
                Map<String, String> map = queryToMap(query);
                String[] from = map.get("from").split(",");
                String[] to = map.get("to").split(",");
                if (from.length == 2 && to.length == 2 &&
                    from[0].matches("-?\\d+") && from[1].matches("-?\\d+") &&
                    to[0].matches("-?\\d+") && to[1].matches("-?\\d+")) {
                    int startX = Integer.parseInt(from[0]);
                    int startY = Integer.parseInt(from[1]);
                    int endX = Integer.parseInt(to[0]);
                    int endY = Integer.parseInt(to[1]);
                    PathfinderResult result = PathLogic.getPath(startX*1600+startY, endX*1600+endY);
                    response = gson.toJson(result);
                }

            }
            Headers headers = t.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}

// this class handles actual pathfinding calculation
class PathLogic {
    // startX, startY give the lower left corner of the map that we care about.
    // everything is then done with that as the origin
    // (as with many things, this needs to be extended when we go outside the surface world)
    static int startX = 1920;
    static int startY = 2560;
    // width and height of our area of the map that we care about
    static int W = 1920;
    static int H = 1600;

    // constants for flags
    static int INFRINGE = 1;
    static int SEEN = 2;
    static int EXTRA = 4;

    // something really important: (READ THIS)
    // for space/computation savings, we represent a game coordinate
    // (x, y) instead as a single integer.
    // (x-startX)*H + (y-startY) + 1
    // why +1? so 0 has some other (null-ish) meaning.

    // shortcuts maps a coordinate to a list of teleports/shortcuts/etc.
    // exact specification of the teleports subject to change.
    static Map<Integer, List<Integer>> shortcuts;

    // starters are the list of teleports you can do from anywhere(ish)
    static ArrayList<Integer> starters;

    // map from coordinate to which squares are walkable from it
    // this is the combination of squares which are entirely unwalkable (nomove.txt)
    // and squares that have some obstacles (walls, fences, etc)
    static byte[] movement;

    static Transport[] transports;

    // shortcut info. this will probably get removed in a refactor
    static int N_INIT;

    // load in the data. done once.
    static void setup() throws IOException {
        Gson gson = new Gson();
        BufferedReader reader = new BufferedReader(new FileReader("transports.json"));
        transports = gson.fromJson(reader, Transport[].class);
        reader.close();

        reader = new BufferedReader(new FileReader("movement.json"));
        Movement movementInput = gson.fromJson(reader, Movement.class);
        reader.close();
        System.out.println(movementInput.walkable.length);
        // obstacles maps a coordinate to a flag saying which directions are walkable
        // only used to create movement map.
        Map<Integer, Integer> obstacles = new HashMap<Integer, Integer>();

        // read in obstacles
        for (int i = 0; i < movementInput.obstaclePositions.length; i++) {
            Coordinate coord = movementInput.obstaclePositions[i];
            if (coord.x < startX || coord.y < startY || coord.x >= (startX + W) || coord.y >= (startY + H)) {
                continue;
            }
            int key = (coord.x - startX) * H + coord.y - startY + 1;
            int val = movementInput.obstacleValues[i];
            obstacles.put(key, val);
        }

        // read in list of unwalkable tiles
        Set<Integer> walkable = new HashSet<Integer>();
        for (Coordinate coord : movementInput.walkable) {
            if (coord.x < startX || coord.y < startY || coord.x >= (startX + W) || coord.y >= (startY + H)) {
                continue;
            }
            int key = (coord.x - startX) * H + coord.y - startY + 1;
            walkable.add(key);
        }

        // put together the movement map
        movement = new byte[W*H+1];
        int TOP = 1;
        int RIGHT = 2;
        int BOTTOM = 4;
        int LEFT = 8;
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                // what obstacles are in my way in N/E/S/W directions?
                int C  = obstacles.getOrDefault(x*H + y+1, 0);

                int N  = (y+1<H) ? obstacles.getOrDefault(x*H + y+1+1, 0) : 15;
                int NE = (y+1<H && x+1<W) ? obstacles.getOrDefault((x+1)*H + y+1+1, 0) : 15;
                int E  = (x+1<W) ? obstacles.getOrDefault((x+1)*H + y+1, 0) : 15;
                int SE = (y-1>-1 && x+1<W) ? obstacles.getOrDefault((x+1)*H + y-1+1, 0) : 15;
                int S  = (y-1>-1) ? obstacles.getOrDefault(x*H + y-1+1, 0) : 15;
                int SW = (y-1>-1 && x-1>-1) ? obstacles.getOrDefault((x-1)*H + y-1+1, 0) : 15;
                int We  = (x-1>-1) ? obstacles.getOrDefault((x-1)*H + y+1, 0) : 15;
                int NW = (y+1<H && x-1>-1) ? obstacles.getOrDefault((x-1)*H + y+1+1, 0) : 15;

                // are these squares blocked?
                boolean bN = !walkable.contains(x*H + y+1+1);
                boolean bNE = !walkable.contains((x+1)*H + y+1+1);
                boolean bE = !walkable.contains((x+1)*H + y+1);
                boolean bSE = !walkable.contains((x+1)*H + y-1+1);
                boolean bS = !walkable.contains(x*H + y-1+1);
                boolean bSW = !walkable.contains((x-1)*H + y-1+1);
                boolean bW = !walkable.contains((x-1)*H + y+1);
                boolean bNW = !walkable.contains((x-1)*H + y+1+1);
                // can I move from my current square to my neighbors? final version
                boolean moveN = ((N & BOTTOM) == 0) & ((C & TOP) == 0) & !bN;
                boolean moveS = ((S & TOP) == 0) & ((C & BOTTOM) == 0) & !bS;
                boolean moveE = ((E & LEFT) == 0) & ((C & RIGHT) == 0) & !bE;
                boolean moveW = ((We & RIGHT) == 0) & ((C & LEFT) == 0) & !bW;
                boolean moveNE = moveN & moveE & ((NE & BOTTOM) == 0) & ((E & TOP) == 0) & ((NE & LEFT) == 0) & ((N & RIGHT) == 0) & !bNE;
                boolean moveSE = moveS & moveE & ((SE & TOP) == 0) & ((E & BOTTOM) == 0) & ((SE & LEFT) == 0) & ((S & RIGHT) == 0) & !bSE;
                boolean moveSW = moveS & moveW & ((SW & TOP) == 0) & ((We & BOTTOM) == 0) & ((SW & RIGHT) == 0) & ((S & LEFT) == 0) & !bSW;
                boolean moveNW = moveN & moveW & ((NW & BOTTOM) == 0) & ((We & TOP) == 0) & ((NW & RIGHT) == 0) & ((N & LEFT) == 0) & !bNW;
                // throw it all into a single value
                int val = (moveN?1:0) + 2*(moveNE?1:0) + 4*(moveE?1:0) + 8*(moveSE?1:0) + 16*(moveS?1:0) + 32*(moveSW?1:0) + 64*(moveW?1:0) + 128*(moveNW?1:0);
                movement[x*H+y+1] = (byte) val;
            }
        }

        // shortcuts garbage -- ignore, this will change
        shortcuts = new HashMap<Integer, List<Integer>>();
        starters = new ArrayList<Integer>();
        for (int i = 0; i < transports.length; i++) {
            Transport t = transports[i];

            if (t.start != null) {
                if (t.start.x > (startX+W) || t.start.x < startX || t.start.y > (startY + H) || t.start.y < startY || t.start.z > 0) {

                    continue;
                }
            }

            if (t.end.x > (startX+W) || t.end.x < startX || t.end.y > (startY + H) || t.end.y < startY || t.end.z > 0) {

                continue;

            }
            Coordinate src = t.start;

            if (src == null) {
                starters.add(i);
            }
            else {
                int srcInt = H*(src.x-startX)+src.y-startY+1;
                if (shortcuts.get(srcInt) == null) {
                    shortcuts.put(srcInt, new ArrayList<Integer>());
                }
                shortcuts.get(srcInt).add(i);
            }
        }
    }

    // IMPORTANT INFO:
    // most Dijkstra-type pathfinders will use a priority queue for the fringe.
    // this is undesirable because we need to update weights for nodes already in the queue,
    // and also because it adds a log(n) cost on top of everything else.
    // instead, we take advantage of having discrete costs, and the fact that we're basically just doing
    // a breadth first search through the walking parts of the map, and just have a "current fringe"
    // and a "next fringe" for the squares that are distance one more away.
    // these are just arrays of constant size, as ArrayLists made this significantly slower.
    // we'll see if a constant fringe size actually is sufficient for all use cases.

    // this simple fringe means you need to be a little more careful with handling teleports that have a non-unit cost
    // so we keep around maps of all the teleports that are going to happen in a "while", putting them in the right place
    // when it's actually time to explore them.
    // also note that technically one unit of "time" here is 0.3 seconds, not 0.6, because you can run 2 squares
    // per tick. everything needs to be relative to that unit.
    // (and we need to be careful about rounding up to the nearest tick when it's time to check teleports, etc.)
    static PathfinderResult getPath(int start, int goal) {
        // just for timing
        double realStartTime = System.nanoTime()/1000000000.0;
        System.out.println("start pathfinder for " + start + "," + goal);
        // convert the start/end to the format we need
        start = start - H*startX - startY+1;
        goal = goal - H*startX - startY+1;

        // these are used to back off to a less good destination if no exact match is found
        int goalX = (goal-1) / H;
        int goalY = (goal-1) - H * goalX;
        int bestChebDistance = 999999;
        int bestManhDistance = 999999;
        int bestNode = -1;

        // map from coord to an explanation (movement id) of how we got there. more explanation coming.
        short[] prev = new short[W*H+1];

        // flags for whether this node is in the fringe, seen, and whether it has shortcuts
        byte[] flags = new byte[W*H+1];
        for (Integer shortcutSrc : shortcuts.keySet()) {
            flags[shortcutSrc] |= EXTRA;
        }

        // book-keeping for getting shortcuts that have been flagged as neighbors of already-visited nodes.
        Map<Integer, List<Integer>> upcoming_special = new HashMap<Integer, List<Integer>>();
        List<Integer> cur_special = new ArrayList<Integer>();
        int cur_special_size = 0;
        int specialptr = 0;


        int FRINGE_SIZE = 10000;
        int[] fringe = new int[]{start, 0};
        int[] nextfringe = new int[FRINGE_SIZE];
        int fringeptr = 0;
        int nextfringeptr = 0;

        // the number of time units (0.3s) used so far.
        int cur_time = 0;
        int loops = 0;
        while (true) {
            loops++;
            int node;
            // if there's a teleport unexplored, use that first
            if (specialptr < cur_special_size) {
                int special_id = cur_special.get(specialptr++);
                Coordinate dest = transports[special_id].end;
                node = H*(dest.x-startX)+dest.y-startY+1;
                if (prev[node] == 0) {
                    prev[node] = (short) (-special_id-1);
                }

            } else {
                // else, get it from the main fringe
                node = fringe[fringeptr++];
            }
            // if we've reached the end, switch fringes
            if (node == 0) {
                if (nextfringe[0] != 0 || upcoming_special.get(cur_time) != null) {
                    // switch fringes and increase time
                    cur_time++;
                    fringe = nextfringe;
                    fringeptr = 0;
                    nextfringeptr = 0;
                    nextfringe = new int[FRINGE_SIZE];
                    cur_special = upcoming_special.get(cur_time);
                    cur_special_size = cur_special == null ? 0 : cur_special.size();
                    specialptr = 0;
                    continue;
                } else {
                    // if the next fringe is empty, there's no path
                    System.out.println("fail " + loops);
                    break;
                }
            }
            // if we've reached the goal, found an optimal path
            if (node == goal) {
                break;
            }

            if ((flags[node] & SEEN) != 0) {
                continue;
            }

            // add to seen
            flags[node] |= SEEN;

            int x = (node-1) / H;
            int y = (node-1) - H * x;
            // update our "closest so far" node in case we can't get exact match
            int goalChebDistance = Math.max(Math.abs(goalX-x), Math.abs(goalY - y));
            if (goalChebDistance <= bestChebDistance) {
                int goalManhDistance = Math.abs(goalX-x) + Math.abs(goalY - y);
                if (goalChebDistance < bestChebDistance || goalManhDistance < bestManhDistance) {
                    bestNode = node;
                    bestChebDistance = goalChebDistance;
                    bestManhDistance = goalManhDistance;
                }
            }
            int mv = movement[node];

            // get the neighbors. doing this in an unrolled loop to save computation time.
            int[] neighbors = new int[8];

            //N
            if (((mv&1) != 0) && y+1 < H) {
                neighbors[0] = H*x + y+1+1;
            }
            //NE
            if (((mv&2) != 0) && x+1 < W && y+1 < H) {
                neighbors[4] = H*(x+1) + y+1+1;
            }
            //E
            if (((mv&4) != 0) && x+1 < W) {
                neighbors[1] = H*(x+1) + y+1;
            }
            //SE
            if (((mv&8) != 0) && x+1 < W && y-1 > -1) {
                neighbors[5] = H*(x+1) + y-1+1;
            }
            //S
            if (((mv&16) != 0) && y-1 > -1) {
                neighbors[2] = H*x + y-1+1;
            }
            //SW
            if (((mv&32) != 0) && x-1 > 0 && y-1 > -1) {
                neighbors[6] = H*(x-1) + y-1+1;
            }
            //W
            if (((mv&64) != 0) && x-1 > 0) {
                neighbors[3] = H*(x-1) + y+1;
            }
            //NW
            if (((mv&128) != 0) && x-1 > 0 && y+1 < H) {
                neighbors[7] = H*(x-1) + y+1+1;
            }


            for (byte i = 0; i < 8; i++) {
                // for each neighbor, skip if it's unreachable or already visited
                // because we're doing it as a BFS with unit cost, we know that if it's even in the fringe, we gain nothing by adding it again.
                int neighbor = neighbors[i];
                if (neighbor == 0) {
                    continue;
                }
                if ((flags[neighbor] & (SEEN | INFRINGE)) != 0) {
                    continue;
                }
                // add to the next fringe
                nextfringe[nextfringeptr++] = neighbor;
                flags[neighbor] |= INFRINGE;
                // set the movement id of the neighbor

                prev[neighbor] = (short)(i+1);
            }

            // if we're just starting, try out the "start-only" teleports.
            if (node == start) {
                for (int i : starters) {
                    int time = ((cur_time+1)/2)*2 + 2 * transports[i].duration;
                    if (upcoming_special.get(time) == null) {
                        upcoming_special.put(time, new ArrayList<Integer>());
                    }
                    upcoming_special.get(time).add(i);
                }
            }

            // check teleports/shortcuts specifically from that node
            if ((flags[node] & EXTRA) != 0 && shortcuts.get(node) != null) {
                for (int i : shortcuts.get(node)) {
                    int time = ((cur_time+1)/2)*2 + 2 * transports[i].duration;
                    if (upcoming_special.get(time) == null) {
                        upcoming_special.put(time, new ArrayList<Integer>());
                    }
                    upcoming_special.get(time).add(i);
                }
            }
        }
        double realSearchTime = System.nanoTime()/1000000000.0 - realStartTime;
        System.out.println("Finished search with " + loops + " loops in " + realSearchTime + " with length of " + cur_time);

        // map from movement id to change in coordinate
        int[] moves = new int[]{1, H, -1, -H, H+1, H-1, -H-1, -H+1};

        // if goal not reached, back off to closest found
        int closestPoint = goal;
        if (prev[goal] == 0) {
            closestPoint = bestNode;
        }

        PathfinderResult result = new PathfinderResult();

        if (closestPoint == start) {
            result.success = false;
            return result;
        }

        result.start = new Coordinate(startX+(start-1)/H, startY+(start-1)%H, 0);
        result.goal = new Coordinate(startX+(goal-1)/H, startY+(goal-1)%H, 0);
        result.totalDuration = 0;
        result.closestPoint = new Coordinate(startX+(closestPoint-1)/H, startY+(closestPoint-1)%H, 0);
        int cur = closestPoint;
        RoutePart part = null;
        while (true) {
            short prevId = prev[cur];
            // if is a shortcut/teleport
            if (prevId < 0) {
                if (part != null) {
                    result.route.add(part);
                }
                part = new RoutePart();
                Transport t = transports[-prevId-1];
                part.duration = t.duration;
                part.title = t.title;
                part.description = t.description;
                part.id = prevId;
                part.coords.add(new Coordinate(startX+(cur-1)/H, startY+(cur-1)%H, 0));
                Coordinate src = t.start;
                // if initial teleport
                if (src == null) {
                    cur = start;
                    part.coords.add(new Coordinate(startX+(cur-1)/H, startY+(cur-1)%H, 0));
                    result.route.add(part);
                    part = null;
                    break;
                } else {
                    cur = H*(src.x-startX)+src.y-startY+1;
                    part.coords.add(new Coordinate(startX+(cur-1)/H, startY+(cur-1)%H, 0));
                    result.route.add(part);
                    part = null;
                    if (cur == start) {
                        break;
                    }
                    continue;
                }
            }
            if (part == null) {
                part = new RoutePart();
                part.duration = 0;
                part.id = 10;
                part.title = "Run";
                part.description = "Run away!";
                part.coords.add(new Coordinate(startX+(cur-1)/H, startY+(cur-1)%H, 0));
            }
            cur = cur - moves[prevId-1];
            part.duration++;
            part.coords.add(new Coordinate(startX+(cur-1)/H, startY+(cur-1)%H, 0));
            if (cur == start) {
                result.route.add(part);
                break;
            }
        }
        for (RoutePart rp : result.route) {
            Collections.reverse(rp.coords);
            if (rp.id == 10) {
                rp.duration = (rp.duration+1)/2;
            }
            result.totalDuration += rp.duration;
        }
        Collections.reverse(result.route);
        double realPathTime = System.nanoTime()/1000000000.0 - realStartTime;
        System.out.println("Finished path with " + "_" + " entries in " + realPathTime + " with path of size " + result.totalDuration);
        result.success = true;
        result.computeTime = realPathTime;
        return result;
    }
}

// these are basically JSON specifications for the return format.
// cooksImplementation.PathfinderResult is the top-level one.
class Coordinate {
    public int x;
    public int y;
    public int z;

    public Coordinate(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

class RoutePart {
    public List<Coordinate> coords;
    public int duration;
    public int id;
    public String title;
    public String description;
    public String image;

    public RoutePart() {
        this.coords = new ArrayList<Coordinate>();
        this.description = "";
        this.image = "";
    }
}

class PathfinderResult {
    public Coordinate start;
    public Coordinate goal;
    public Coordinate closestPoint;
    public boolean success;
    public String message;
    public int totalDuration;
    public double computeTime;
    public List<RoutePart> route;

    public PathfinderResult() {
        this.route = new ArrayList<RoutePart>();
    }
}

class Transport {
    public int id;
    public String title;
    public String description;
    public String image;
    public int duration;
    public Coordinate start;
    public Coordinate end;
    public String[] requirements;
}

class Movement {
    Coordinate[] walkable;
    Coordinate[] obstaclePositions;
    int[] obstacleValues;
}
