import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all score persistence — reading and writing scores.txt.
 * No game logic lives here; this class only knows about the file.
 */
public class ScoreRepository {

    private static final String FILE_PATH = "scores.txt";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Writes a score with timestamp to the score file
     *
     * @param score the score to save
     */
    public void saveScore(int score) {
        try (FileWriter fw = new FileWriter(FILE_PATH, true)) {
            fw.write(score + ";" + LocalDateTime.now().format(FORMATTER) + "\n");
        } catch (IOException e) {
            System.err.println("Failed to save score: " + e.getMessage());
        }
    }

    /**
     * Loads all score entries from the score file
     *
     * @return list of score entries
     */
    public List<String> loadAllScores() {
        List<String> scores = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return scores;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) scores.add(line);
            }
        } catch (IOException e) {
            System.err.println("Failed to load scores: " + e.getMessage());
        }
        return scores;
    }

    /**
     * Finds the highest score from all saved entris
     *
     * @return the highest score, or 0 if none exist or none could be parsed
     * */
    public int getHighestScore() {
        return loadAllScores().stream()
                .map(line -> line.split(";")[0])
                .mapToInt(s -> {
                    try { return Integer.parseInt(s); }
                    catch (NumberFormatException e) { return 0; }
                })
                .max()
                .orElse(0);
    }
}