/*
 * TicTacToeNetworked.java
 * -------------------------
 * Console-based Tic Tac Toe game featuring:
 *  - Unbeatable AI (Minimax Algorithm)
 *  - Local multiplayer (2 players on same computer)
 *  - Online multiplayer (via TCP networking)
 *
 * -------------------------
 * HOW TO RUN IN INTELLIJ IDEA:
 * 1️⃣ Create a new Java project in IntelliJ.
 * 2️⃣ In the `src` folder, create a new Java class named `TicTacToeNetworked`.
 * 3️⃣ Replace the entire content with this code.
 * 4️⃣ Click the green ▶️ button to run, then in Run Configuration > Program Arguments, set one of the following:
 *    - ai → Play vs AI (You = X)
 *    - local → Local 2-player
 *    - host 12345 → Host a network game on port 12345
 *    - join 127.0.0.1 12345 → Join a hosted game (replace IP & port)
 * 5️⃣ For online play, host and join from two IntelliJ windows or two PCs on the same network.
 * -------------------------
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class TicTacToeNetworked {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: java TicTacToeNetworked [ai|local|host <port>|join <host> <port>]");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "ai":
                new Game(new ConsolePlayer('X'), new AIPlayer('O')).playLocal();
                break;
            case "local":
                new Game(new ConsolePlayer('X'), new ConsolePlayer('O')).playLocal();
                break;
            case "host":
                if (args.length < 2) { System.out.println("Provide port: host <port>"); return; }
                new Server(Integer.parseInt(args[1])).start();
                break;
            case "join":
                if (args.length < 3) { System.out.println("Provide host and port: join <host> <port>"); return; }
                new Client(args[1], Integer.parseInt(args[2])).start();
                break;
            default:
                System.out.println("Invalid mode.");
        }
    }

    // ---------- Board ----------
    static class Board {
        char[] grid = new char[9];
        Board() { Arrays.fill(grid, ' '); }
        Board(Board other) { this.grid = Arrays.copyOf(other.grid, 9); }

        boolean isEmpty(int i) { return grid[i] == ' '; }
        boolean makeMove(int i, char player) {
            if (i < 0 || i >= 9 || !isEmpty(i)) return false;
            grid[i] = player; return true;
        }
        void undo(int i) { grid[i] = ' '; }

        List<Integer> availableMoves() {
            List<Integer> moves = new ArrayList<>();
            for (int i = 0; i < 9; i++) if (isEmpty(i)) moves.add(i);
            return moves;
        }

        char checkWinner() {
            int[][] w = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
            for (int[] c : w) if (grid[c[0]]!=' ' && grid[c[0]]==grid[c[1]] && grid[c[1]]==grid[c[2]]) return grid[c[0]];
            for (char c: grid) if (c==' ') return ' ';
            return 'D';
        }

        String render() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 9; i++) {
                sb.append(' ').append(grid[i]==' '?(i+1):grid[i]).append(' ');
                if (i % 3 != 2) sb.append('|');
                if (i % 3 == 2 && i != 8) sb.append("\n---+---+---\n");
            }
            return sb.toString();
        }

        String compact() { return new String(grid); }
        static Board fromCompact(String s) {
            Board b = new Board();
            for (int i=0;i<9 && i<s.length();i++) b.grid[i] = s.charAt(i);
            return b;
        }
    }

    // ---------- Player Interfaces ----------
    interface Player { int nextMove(Board b, char mark); String name(); }

    static class ConsolePlayer implements Player {
        Scanner sc = new Scanner(System.in); char mark;
        ConsolePlayer(char m) { mark=m; }
        public int nextMove(Board b, char mark) {
            while (true) {
                System.out.println(b.render());
                System.out.print("Player "+mark+", enter move (1-9): ");
                try {
                    int mv = Integer.parseInt(sc.nextLine().trim())-1;
                    if (mv>=0 && mv<9 && b.isEmpty(mv)) return mv;
                } catch(Exception ignored){}
                System.out.println("Invalid move, try again.");
            }
        }
        public String name() { return "Console("+mark+")"; }
    }

    static class AIPlayer implements Player {
        char mark; Minimax mini;
        AIPlayer(char m) { mark=m; mini=new Minimax(m); }
        public int nextMove(Board b, char mark) {
            int move = mini.bestMove(b);
            System.out.println("AI chooses "+(move+1));
            return move;
        }
        public String name() { return "AI("+mark+")"; }
    }

    // ---------- Minimax Algorithm ----------
    static class Minimax {
        char ai, human;
        Minimax(char ai) { this.ai=ai; this.human=(ai=='X'?'O':'X'); }

        int bestMove(Board b) {
            int best = Integer.MIN_VALUE, move=-1;
            for (int m: b.availableMoves()) {
                b.makeMove(m, ai);
                int score = minimax(b, false);
                b.undo(m);
                if (score>best) { best=score; move=m; }
            }
            return move;
        }

        int minimax(Board b, boolean isMax) {
            char res=b.checkWinner();
            if (res==ai) return 10;
            if (res==human) return -10;
            if (res=='D') return 0;

            int best = isMax?Integer.MIN_VALUE:Integer.MAX_VALUE;
            for (int m:b.availableMoves()) {
                b.makeMove(m, isMax?ai:human);
                int score=minimax(b,!isMax);
                b.undo(m);
                best=isMax?Math.max(best,score):Math.min(best,score);
            }
            return best;
        }
    }

    // ---------- Local Game ----------
    static class Game {
        Player p1,p2;
        Game(Player a, Player b) { p1=a; p2=b; }
        void playLocal() {
            Board b = new Board();
            char turn='X';
            while (true) {
                Player cur = (turn=='X')?p1:p2;
                int mv = cur.nextMove(b, turn);
                b.makeMove(mv, turn);
                char res=b.checkWinner();
                if (res=='X'||res=='O') { System.out.println(b.render()); System.out.println("Player "+res+" wins!"); break; }
                if (res=='D') { System.out.println(b.render()); System.out.println("Draw!"); break; }
                turn=(turn=='X')?'O':'X';
            }
        }
    }

    // ---------- Networking Server ----------
    static class Server {
        int port; Server(int p){port=p;}
        void start(){
            try(ServerSocket ss=new ServerSocket(port)){
                System.out.println("Hosting game on port "+port+"...");
                try(Socket s=ss.accept()){
                    System.out.println("Client connected: "+s.getInetAddress());
                    handle(s);
                }
            }catch(Exception e){e.printStackTrace();}
        }

        void handle(Socket s)throws Exception{
            BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream()));
            PrintWriter out=new PrintWriter(s.getOutputStream(),true);
            Scanner sc=new Scanner(System.in);
            Board b=new Board();
            char turn='X';
            out.println("BOARD "+b.compact());

            while(true){
                if(turn=='X'){
                    System.out.println(b.render());
                    System.out.print("Your move (1-9): ");
                    int mv=Integer.parseInt(sc.nextLine())-1;
                    if(!b.makeMove(mv,'X'))continue;
                    out.println("MOVE "+mv);
                }else{
                    String line=in.readLine();
                    if(line==null)break;
                    if(line.startsWith("MOVE")){
                        int mv=Integer.parseInt(line.split(" ")[1]);
                        b.makeMove(mv,'O');
                    }
                }

                out.println("BOARD "+b.compact());
                char res=b.checkWinner();
                if(res!=' '){out.println("RESULT "+res);System.out.println(b.render());System.out.println(res=='D'?"Draw!":"Winner: "+res);break;}
                turn=(turn=='X')?'O':'X';
            }
        }
    }

    // ---------- Networking Client ----------
    static class Client {
        String host;int port;Client(String h,int p){host=h;port=p;}
        void start(){
            try(Socket s=new Socket(host,port)){
                BufferedReader in=new BufferedReader(new InputStreamReader(s.getInputStream()));
                PrintWriter out=new PrintWriter(s.getOutputStream(),true);
                Scanner sc=new Scanner(System.in);
                Board b=new Board();

                while(true){
                    String line=in.readLine();
                    if(line==null)break;
                    if(line.startsWith("BOARD")){
                        b=Board.fromCompact(line.substring(6));
                        System.out.println(b.render());
                    }else if(line.startsWith("RESULT")){
                        char r=line.split(" ")[1].charAt(0);
                        System.out.println(r=='D'?"Draw!":"Winner: "+r);break;
                    }

                    int xCount=0,oCount=0;for(char c:b.grid){if(c=='X')xCount++;else if(c=='O')oCount++;}
                    char turn=(xCount==oCount)?'X':'O';
                    if(turn=='O'){
                        System.out.print("Your move (1-9): ");
                        int mv=Integer.parseInt(sc.nextLine())-1;
                        out.println("MOVE "+mv);
                    }
                }
            }catch(Exception e){e.printStackTrace();}
        }
    }
}

