import java.io.*;
import java.net.*;
import java.util.Scanner;

public class GuessClient {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Socket socket = new Socket("3.95.228.78", 8888);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                if (line.trim().startsWith("Entrez votre identifiant")) break;
            }

            String clientId = scanner.nextLine();
            out.println(clientId);

            Thread listener = new Thread(() -> {
                try {
                    String serverLine;
                    while ((serverLine = in.readLine()) != null) {
                        System.out.println(serverLine);
                        if (serverLine.equalsIgnoreCase("BYE")) break;
                    }
                } catch (IOException ignored) {}
            });
            listener.start();

            while (true) {
                String input = scanner.nextLine();
                out.println(input);
                if (input.equalsIgnoreCase("QUIT")) break;
            }

        } catch (IOException e) {
            System.out.println("Connexion échouée : " + e.getMessage());
        }
    }
}