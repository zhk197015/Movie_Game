import lombok.extern.slf4j.Slf4j;
import model.tmdb.Movie;
import controller.GameController;
import model.game.GameSession;
import model.game.WinCondition;
import service.movie.MovieDataService;
import service.movie.MovieDataServiceImpl;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
public class Main {
    public static void main(String[] args) {
        try {
            // After input, proceed to initialize game
            MovieDataService movieDataService = MovieDataServiceImpl.getInstance();

//            Movie startMovie = movieDataService.searchMoviesByPrefix("Inception").stream()
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("Start movie not found"));
            
            

            Movie startMovie = movieDataService.getRandomStarterMovie();

            

            WinCondition placeholder1 = new WinCondition("genre", "Action", 3);
            WinCondition placeholder2 = new WinCondition("genre", "Comedy", 4);
            
            Screen screen = new DefaultTerminalFactory().createScreen();
            screen.startScreen();
            MultiWindowTextGUI gui = new MultiWindowTextGUI(screen, new DefaultWindowManager(), new EmptySpace());

            // TextBoxes for user input
            TextBox player1Input = new TextBox().setPreferredSize(new TerminalSize(30, 1));
            TextBox player2Input = new TextBox().setPreferredSize(new TerminalSize(30, 1));
            AtomicReference<String> player1Name = new AtomicReference<>("");
            AtomicReference<String> player2Name = new AtomicReference<>("");

            // Input panel
            Panel inputPanel = new Panel();
            inputPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
            inputPanel.addComponent(new Label("Enter Player 1 Name:"));
            inputPanel.addComponent(player1Input);
            inputPanel.addComponent(new Label("Enter Player 2 Name:"));
            inputPanel.addComponent(player2Input);

            BasicWindow inputWindow = new BasicWindow("Player Setup");
            inputWindow.setComponent(inputPanel);

            Button submitButton = new Button("Start Game", () -> {
                player1Name.set(player1Input.getText().trim());
                player2Name.set(player2Input.getText().trim());
                if (!player1Name.get().isEmpty() && !player2Name.get().isEmpty()) {
                    inputWindow.close();
                }
            });
            inputPanel.addComponent(submitButton);

            gui.addWindowAndWait(inputWindow);

            GameSession session = new GameSession("session-001", startMovie, placeholder1, placeholder2, player1Name.get(), player2Name.get());

            GameController controller = new GameController(session, movieDataService);
            controller.startGame();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
