package wiki.runescape.oldschool.pathfinder;

/**
 * Prints activities to the console and logs their times
 */
public class TimeLogger {
    private long startTime;
    private long previousLapTime;
    private String prefix;

    public void start(String logPrefix) {
        this.prefix = logPrefix;
        this.previousLapTime = this.startTime = System.currentTimeMillis();
    }

    public void lap(String substep) {
        final long lapTime = System.currentTimeMillis() - previousLapTime;
        System.out.println(this.prefix + ": " + substep + ": " + lapTime + "ms");
        this.previousLapTime = System.currentTimeMillis();
    }


    public void end() {
        final long totalTime = System.currentTimeMillis() - this.startTime;
        System.out.println(this.prefix + ": total: " + totalTime + "ms");
    }


}
