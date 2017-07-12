import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

/**
 * Created by cdxu0 on 2017/7/12.
 */
public class TicTacToeServer extends Application implements TicTacToeConstants{
    private int sessionNo = 1;

    @Override
    public void start(Stage primaryStage) {
        TextArea taLog = new TextArea();
        //create a scene and place it in the stage
        Scene scene = new Scene(new ScrollPane(taLog), 450, 200);
        primaryStage.setTitle("TicTacToeServer");
        primaryStage.setScene(scene);
        primaryStage.show();

        new Thread( ()-> {
            try {
                //create a server socket
                ServerSocket serverSocket = new ServerSocket(8000);
                Platform.runLater(() -> taLog.appendText(new Date()
                        + "server started at socket 8000\n"));
                while (true) {
                    Platform.runLater(()-> taLog.appendText(new Date() + ": Wait for players to join session" + sessionNo + "\n"));
                    //connect to player1
                    Socket player1 = serverSocket.accept();
                    Platform.runLater(()-> {
                        taLog.appendText(new Date() + ": player1 joined session " + sessionNo+ "\n");
                        taLog.appendText("Player1 's IP address" + player1.getInetAddress().getHostAddress() + "\n");
                    });
                    new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);
                    //connect to player2
                    Socket player2 = serverSocket.accept();
                    Platform.runLater(()-> {
                        taLog.appendText(new Date() + ": player2 joined session " + sessionNo+ "\n");
                        taLog.appendText("Player2 's IP address" + player2.getInetAddress().getHostAddress() + "\n");
                    });
                    //Notify that the player is player2
                    new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);
                    //display this session and increment session number
                    Platform.runLater(()->{
                        taLog.appendText(new Date() + ": Start a thread for session" + sessionNo++ + "\n");
                    });
                    new Thread(new HandleASession(player1,player2)).start();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    class HandleASession implements Runnable, TicTacToeConstants {
        private Socket player1;
        private Socket player2;

        private char[][] cell = new char[3][3];
        private DataInputStream fromPlayer1;
        private DataOutputStream toPlayer1;
        private DataInputStream fromPlayer2;
        private DataOutputStream getToPlayer2;

        //Continue to play
        private boolean continueToPlay = true;
        //Construct a thread
        public HandleASession(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    cell[i][j] = ' ';
        }

        //implement thr run() method for the thread
        public void run() {
            try {
                //create data input and output stream
                DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
                DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
                DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
                DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());

                toPlayer1.writeInt(1);
                while (true) {
                    int row = fromPlayer1.readInt();
                    int column = fromPlayer1.readInt();
                    cell[row][column] = 'X';
                    //check if player1 wins
                    if (isWon('X')) {
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);
                        sendMove(toPlayer2, row, column);
                        break;
                    } else if (isFull()) {
                        toPlayer1.writeInt(DRAW);
                        toPlayer2.writeInt(DRAW);
                        sendMove(toPlayer2, row, column);
                        break;
                    } else {
                        toPlayer2.writeInt(CONTINUE);
                        sendMove(toPlayer2, row, column);
                    }

                    row = fromPlayer2.readInt();
                    column = fromPlayer2.readInt();
                    cell[row][column] = 'O';

                    if (isWon('O')){
                        toPlayer1.writeInt(PLAYER2_WON);
                        toPlayer2.writeInt(PLAYER2_WON);
                        sendMove(toPlayer1, row, column);
                        break;
                    } else {
                        toPlayer1.writeInt(CONTINUE);
                        sendMove(toPlayer1, row, column);
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        //send the move to other player
        private void sendMove(DataOutputStream out, int row, int column) throws IOException{
            out.writeInt(row);
            out.writeInt(column);
        }
        //determine if the cells are all occupied
        private boolean isFull() {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (cell[i][j] == ' ')
                        return false;
            return true;
        }
        //determine if the player with the specific token wins
        private boolean isWon(char token) {
            for (int i = 0 ; i < 3; i++)
                if ((cell[i][0] == token) && (cell[i][1] == token) && (cell[i][2] == token))
                    return true;

            for (int j = 0; j < 3; j++) {
                if ((cell[0][j] == token) && (cell[1][j] == token) && (cell[2][j] == token))
                    return true;
            }

            if ((cell[0][0] == token) && (cell[1][1] == token) && (cell[2][2] == token))
                return true;

            if ((cell[0][2] == token) && (cell[1][1] == token) && (cell[2][0] == token))
                return true;
            return false;
        }
    }
 }
