package view;

import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.concurrent.*;
import model.game.Connection;
import model.game.HistoryRecord;
import model.game.WinCondition;
import model.tmdb.CastMember;
import model.tmdb.CrewMember;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.movie.MovieDataService;
import service.movie.MovieGenreService;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;


public class ConsoleView {
    private final MultiWindowTextGUI gui;
    private ScheduledExecutorService scheduler;
    private volatile boolean timerRunning;
    private volatile int secondsRemaining = 30;

    public ConsoleView() throws IOException {
        Screen screen = new DefaultTerminalFactory().createScreen();
        screen.startScreen();
        gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());
    }

    public void showWelcome() {
        showMessage("Welcome to the Movie Name Game!");
    }

    public void showWinCondition(String condition) {
        showMessage("Win condition: " + condition);
    }

    public void showCurrentRound(int step) {
        showMessage("=== Round " + step + " ===");
    }

    public void showCurrentPlayer(String playerName) {
        showMessage(">>> Now it's " + playerName + "'s turn <<<");
    }
    
    public void showlost() {
        showMessage("Time is up");
    }

    private void showMessage(String message) {
        BasicWindow window = new BasicWindow();
        Panel contentPanel = new Panel();
        contentPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
        contentPanel.addComponent(new Label(message));
        contentPanel.addComponent(new Button("OK", window::close));
        window.setComponent(contentPanel);
        gui.addWindowAndWait(window);
    }

    public void stop() throws IOException {
        gui.getScreen().stopScreen();
    }
    
    public void showError(String message) {
        BasicWindow window = new BasicWindow("Error");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("Error: " + message));
        panel.addComponent(new Button("OK", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
    }
    
    public void showVictory() {
        showMessage("🎉 You won! You met the win condition!");
    }
    
    public void showRecentHistory(List<HistoryRecord> history) {
        BasicWindow window = new BasicWindow("Recent History");
        Panel panel = new Panel();
        panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("── Recent History (last 5 turns) ──"));

        if (history.isEmpty()) {
            panel.addComponent(new Label("No history yet."));
        } else {
            int start = Math.max(0, history.size() - 5);

            for (int i = start; i < history.size(); i++) {
                HistoryRecord record = history.get(i);
                Movie movie = record.getMovie();

                // Safe release year
                String year = "Unknown";
                String releaseDate = movie.getReleaseDate();
                if (releaseDate != null && releaseDate.contains("-")) {
                    year = releaseDate.split("-")[0];
                }

                // Safe genre names
                int[] genreIds = movie.getGenreIds();
                String[] genreNames;
                if (genreIds == null || genreIds.length == 0) {
                    genreNames = new String[]{"Unknown"};
                } else {
                    genreNames = Arrays.stream(genreIds)
                            .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                            .toArray(String[]::new);
                }

                // Connection if not first item
                if (i > start) {
                    Connection conn = record.getConnection();
                    if (conn != null) {
                        panel.addComponent(new Label("      |"));
                        panel.addComponent(new Label("Connection: " + conn.getConnectionValue()));
                        panel.addComponent(new Label("      |"));
                    }
                }

                panel.addComponent(new Label("▶ " + movie.getTitle() + " (" + year + "), Genres: " + String.join(", ", genreNames)));
            }
        }

        panel.addComponent(new EmptySpace(new TerminalSize(1, 1)));
        panel.addComponent(new Button("OK", window::close));

        window.setComponent(panel);
        gui.addWindowAndWait(window);
    }

    public void startCountdownTimer(int seconds, Runnable onTimeout, Label timerLabel) {
        secondsRemaining = seconds;
        timerRunning = true;

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            if (timerRunning && secondsRemaining > 0) {
                secondsRemaining--;
                timerLabel.setText("Time left: " + secondsRemaining + "s");
            } else if (secondsRemaining == 0) {
                timerRunning = false;
                scheduler.shutdownNow();
                onTimeout.run(); // Timeout action
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    public String showGameTurn(
            
            int round,
            String playerName,
            List<HistoryRecord> history,
            Movie movie,
            WinCondition condition,
            MovieDataService movieDataService,
            Runnable onTimeout,
            Label timerLabel,
            boolean startTimer
    ) {
        BasicWindow window = new BasicWindow("Movie Game Turn");
        Panel panel = new Panel(new LinearLayout(Direction.VERTICAL));

        panel.addComponent(new Label("=== Round " + round + " ==="));
        panel.addComponent(new Label("Current Player: " + playerName));
        panel.addComponent(new Label("── Recent History ──"));

        int start = Math.max(0, history.size() - 5);
        for (int i = start; i < history.size(); i++) {
            HistoryRecord record = history.get(i);
            Movie m = record.getMovie();

            String year = (m.getReleaseDate() != null && m.getReleaseDate().contains("-"))
                    ? m.getReleaseDate().split("-")[0] : "Unknown";

            int[] genreIds = m.getGenreIds();
            String[] genres = (genreIds == null || genreIds.length == 0)
                    ? new String[]{"Unknown"}
                    : Arrays.stream(genreIds)
                            .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                            .toArray(String[]::new);

            if (i > start && record.getConnection() != null) {
                panel.addComponent(new Label("  |"));
                panel.addComponent(new Label("Connection: " + record.getConnection().getConnectionValue()));
                panel.addComponent(new Label("  |"));
            }

            panel.addComponent(new Label("▶ " + m.getTitle() + " (" + year + "), Genres: " + String.join(", ", genres)));
        }

        panel.addComponent(new Label("── Current Movie Info ──"));
        String movieYear = (movie.getReleaseDate() != null && movie.getReleaseDate().contains("-"))
                ? movie.getReleaseDate().split("-")[0] : "Unknown";
        panel.addComponent(new Label(movie.getTitle() + " (" + movieYear + ")"));

        int[] currentGenres = movie.getGenreIds();
        String[] movieGenres = (currentGenres == null || currentGenres.length == 0)
                ? new String[]{"Unknown"}
                : Arrays.stream(currentGenres)
                        .mapToObj(id -> MovieGenreService.getInstance().getGenreName(id))
                        .toArray(String[]::new);
        panel.addComponent(new Label("Genres: " + String.join(", ", movieGenres)));

        MovieCredits credits = movieDataService.getMovieCredits(movie.getId());
        if (credits != null) {
            addCrew(panel, "Director", credits.getCrew(), "Director");
            addCrew(panel, "Writer", credits.getCrew(), "Writer", "Screenplay");
            addCrew(panel, "Cinematographer", credits.getCrew(), "Cinematographer", "Director of Photography");
            addCrew(panel, "Composer", credits.getCrew(), "Composer", "Original Music Composer");

            List<String> cast = credits.getCast() != null
                    ? credits.getCast().stream().limit(6).map(CastMember::getName).toList()
                    : List.of();

            if (!cast.isEmpty()) {
                panel.addComponent(new Label("Featuring: " + String.join(", ", cast)));
            }
        } else {
            panel.addComponent(new Label("(Credits not available)"));
        }

        panel.addComponent(new Label("Progress: " + condition.getConditionValue()+ ": " +condition.getCurrentCount() + " / " + condition.getTargetCount()));
        panel.addComponent(new Label("Enter the next movie prefix:"));

        TextBox inputBox = new TextBox().setPreferredSize(new TerminalSize(40, 1));
        panel.addComponent(inputBox);

        Label usedTimerLabel = timerLabel != null ? timerLabel : new Label("Time left: " + secondsRemaining + "s");
        panel.addComponent(usedTimerLabel);

        panel.addComponent(new Label("Suggestions:"));
        ComboBox<String> movieComboBox = new ComboBox<>();
        movieComboBox.setPreferredSize(new TerminalSize(40, 1));
        panel.addComponent(movieComboBox);

        Button submitButton = new Button("Submit", window::close);
        panel.addComponent(submitButton);

        inputBox.setTextChangeListener((newText, changedByUserInteraction) -> {
            movieComboBox.clearItems();
            if (!newText.trim().isEmpty()) {
                List<Movie> suggestions = movieDataService.searchMoviesByPrefix(newText.trim());
                for (Movie m : suggestions) {
                    movieComboBox.addItem(m.getTitle());
                }
            }
            try {
                gui.updateScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        
        if (startTimer) {
            startCountdownTimer(secondsRemaining, () -> {
                window.close();
                onTimeout.run();
            }, usedTimerLabel);
        }

        window.setComponent(panel);
        gui.addWindowAndWait(window);

        timerRunning = false;
        if (scheduler != null) scheduler.shutdownNow();
           
       
        String selectedTitle = movieComboBox.getSelectedItem();
            
        return selectedTitle != null ? selectedTitle.trim() : "";
    }

    private void addCrew(Panel panel, String label, List<CrewMember> crewList, String... jobs) {
        if (crewList == null) return;

        List<String> names = crewList.stream()
                .filter(c -> Arrays.asList(jobs).contains(c.getJob()))
                .map(CrewMember::getName)
                .toList();

        if (!names.isEmpty()) {
            panel.addComponent(new Label(label + ": " + String.join(", ", names)));
        }
    }    
    
    public void showErrorNonBlocking(String message) {
        new Thread(() -> {
            try {
                gui.getGUIThread().invokeLater(() -> {
                    BasicWindow window = new BasicWindow("Error");
                    Panel panel = new Panel();
                    panel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
                    panel.addComponent(new Label("Error: " + message));
                    panel.addComponent(new Button("OK", window::close));
                    window.setComponent(panel);
                    gui.addWindow(window);
                });
            } catch (Exception ignored) {}
        }).start();
    }
    
    public void resettime() {
        this.secondsRemaining = 30;
    }
}