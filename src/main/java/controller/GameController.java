package controller;

import model.game.Connection;
import model.game.GameSession;
import model.game.WinCondition;
import model.tmdb.Movie;
import model.tmdb.MovieCredits;
import service.movie.MovieDataService;
import service.movie.MovieDataServiceImpl;
import service.movie.MovieGenreService;
import view.ConsoleView;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.googlecode.lanterna.gui2.Label;

import java.io.IOException;

public class GameController {
    private final GameSession session;
    private final MovieDataService movieDataService;
    private ConsoleView view;

    public GameController(GameSession session, MovieDataService movieDataService) {
        this.session = session;
        this.movieDataService = movieDataService;
    }

    private WinCondition getRandomGenreWinConditiontion(int targetc) {
        Map<Integer, String> genreMap = MovieGenreService.getInstance().getAllGenreMap();
        List<String> genreNames = genreMap.values().stream().sorted().toList();
        String randomGenre = genreNames.get(new Random().nextInt(genreNames.size()));
        return new WinCondition("genre", randomGenre, targetc);
    }

    public void startGame() {
        try {
            this.view = new ConsoleView();
            view.showWelcome();
            
            int targetCount = new Random().nextInt(5) + 3;

            WinCondition player1Condition = getRandomGenreWinConditiontion(targetCount);
            //WinCondition player1Condition = new WinCondition("genre", "Action", 1);
            WinCondition player2Condition = getRandomGenreWinConditiontion(targetCount);
            //WinCondition player2Condition = new WinCondition("genre", "Action", 1);
            session.setPlayer1WinCondition(player1Condition);
            session.setPlayer2WinCondition(player2Condition);

            view.showWinCondition(session.getPlayer1Name() + ": " + player1Condition.getConditionValue() + ", " + player1Condition.getTargetCount() + " times");
            view.showWinCondition(session.getPlayer2Name() + ": " + player2Condition.getConditionValue() + ", " + player2Condition.getTargetCount() + " times");

            Movie currentMovie = session.getCurrentMovie();
            if (session.getRecentHistory().isEmpty()) {
                session.addInitialMovieToHistory(currentMovie);
            }
            
            boolean firstAttempt = true;
            Label timerLabel = new Label("Time left: 30s");

            while (!session.hasWon()) {
                String selectedTitle = view.showGameTurn(
                        session.getCurrentStep(),
                        session.getCurrentPlayerName(),
                        session.getRecentHistory(),
                        currentMovie,
                        session.getCurrentPlayerWinCondition(),
                        movieDataService,
                        () -> {
                            try {
                                view.stop();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            System.out.println("\u23F0 Time's up! " + session.getCurrentPlayerName() + " lost the game.");
                            System.exit(0);
                        },
                        timerLabel,
                        firstAttempt
                );
                
                Movie selected = movieDataService.searchMoviesByPrefix(selectedTitle).stream()
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("selected movie not found"));
                

                if (selected == null) {
                    view.showErrorNonBlocking("Movie not found or selection was invalid.");
                    continue;
                }

                if (movieDataService.isMovieAlreadyUsed(selected, session)) {
                    view.showErrorNonBlocking("You already used this movie.");
                    continue;
                }

                if (!movieDataService.validateConnection(currentMovie, selected)) {
                    view.showErrorNonBlocking("No valid connection between movies.");
                    continue;
                }
                
                timerLabel = new Label("Time left: 30s");
                view.resettime();
                

                movieDataService.registerUsedMovie(selected, session);

                List<Connection> connections = movieDataService.getConnections(currentMovie, selected);
                boolean connectionRegistered = false;

                for (Connection conn : connections) {
                    if (!movieDataService.isConnectionUsedThreeTimes(conn, session)) {
                        movieDataService.registerUsedConnection(conn, session);
                        session.addToHistory(selected, conn);
                        connectionRegistered = true;
                        break;
                    }
                }

                if (!connectionRegistered) {
                    view.showError("All available connections between the movies have been used 3 times.");
                    continue;
                }

                WinCondition condition = session.getCurrentPlayerWinCondition();
                if (movieDataService.matchesWinCondition(selected, condition)) {
                    condition.incrementProgress();
                }

                
                if (session.hasWon()) {
                    break;
                }
                currentMovie = selected;
                session.switchTurn();
                firstAttempt = true;
            }
            view.stop();
            System.out.println("\uD83C\uDF89" + session.getCurrentPlayerName() +" " +"won! You met the win condition!");
            System.out.flush();
            System.exit(0);

        } catch (IOException e) {
            System.err.println("Error initializing Lanterna screen: " + e.getMessage());
        } catch (Exception e) {
            if (view != null) view.showError("Unexpected error: " + e.getMessage());
            else System.err.println("Unexpected error: " + e.getMessage());
        }
    }
}