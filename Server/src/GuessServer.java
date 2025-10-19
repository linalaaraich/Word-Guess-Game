import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuessServer {
    private static final int PORT = 8888;
    private static final int WORD_LENGTH = 5;

    private static final Map<String, String> WORD_HINTS = new LinkedHashMap<>();
    static {
        WORD_HINTS.put("CHIEN", "animal domestique");
        WORD_HINTS.put("POMME", "fruit rouge");
        WORD_HINTS.put("TABLE", "meuble");
        WORD_HINTS.put("LIVRE", "pour lire");
        WORD_HINTS.put("PORTE", "entrée");
        WORD_HINTS.put("LAMPE", "éclaire");
        WORD_HINTS.put("JAUNE", "couleur");
        WORD_HINTS.put("SABLE", "plage");
        WORD_HINTS.put("FLEUR", "plante");
        WORD_HINTS.put("ROUTE", "chemin");
        WORD_HINTS.put("PIANO", "instrument");
        WORD_HINTS.put("ECOLE", "apprentissage");
    }

    private volatile String currentSecret;
    private volatile String currentHint;
    private volatile boolean roundActive = true;
    private final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean victoryAnnounced = new AtomicBoolean(false);
    private final AtomicBoolean waitingForReplay = new AtomicBoolean(false);

    public static void main(String[] args) {
        System.out.println("Serveur Guess de Word démarré sur le port " + PORT);
        new GuessServer().start();
    }

    public GuessServer() {
        pickNewSecret();
    }

    private void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("En attente de connexions...");
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur : " + e.getMessage());
        }
    }

    private void pickNewSecret() {
        List<String> words = new ArrayList<>(WORD_HINTS.keySet());
        Random r = new Random();
        currentSecret = words.get(r.nextInt(words.size()));
        currentHint = WORD_HINTS.get(currentSecret);
        victoryAnnounced.set(false);
        roundActive = true;
        waitingForReplay.set(false);
        System.out.println("Nouveau mot secret : " + currentSecret + " → " + currentHint);
        broadcast("NEWROUND WORDLEN " + WORD_LENGTH);
        broadcast("Indice : " + currentHint);
        broadcast("Utilisez : TRY <mot> ou QUIT");
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    class ClientHandler implements Runnable {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private String clientId = null;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void sendMessage(String msg) {
            if (!socket.isClosed()) {
                out.print(msg);
                out.println();
            }
        }

        @Override
        public void run() {
            try {
                sendMessage("- RÈGLES DU JEU : ");
                sendMessage("Devinez un mot secret de 5 lettres.");
                sendMessage("À chaque essai, vous recevrez un retour par position :");
                sendMessage(" - OK_POS : bonne lettre à la bonne position");
                sendMessage(" - OK_MIS : bonne lettre à la mauvaise position");
                sendMessage(" - WRONG   : lettre absente du mot");

                String id = null;
                do {
                    sendMessage("Entrez votre identifiant : ");
                    id = in.readLine();
                } while (id == null || id.trim().isEmpty());

                id = id.trim().replaceAll("[^a-zA-Z0-9]", "_");
                this.clientId = id;

                broadcast("BROADCAST 🟢 [" + clientId + "] a rejoint la partie.");
                System.out.println("🟢 [" + clientId + "] connecté.");

                sendMessage("WELCOME " + clientId);
                sendMessage("WORDLEN " + WORD_LENGTH);
                sendMessage("HINT " + currentHint);
                sendMessage("Utilisez : TRY <mot> ou QUIT");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (line.equalsIgnoreCase("QUIT")) {
                        sendMessage("BYE");
                        break;
                    }

                    if (!roundActive && waitingForReplay.get()) {
                        if (line.equalsIgnoreCase("OUI")) {
                            pickNewSecret();
                            continue;
                        } else if (line.equalsIgnoreCase("QUIT")) {
                            sendMessage("BYE");
                            break;
                        } else {
                            sendMessage("Répondez par OUI pour rejouer ou QUIT pour quitter.");
                            continue;
                        }
                    }

                    if (line.toUpperCase().startsWith("TRY ")) {
                        if (!roundActive && !waitingForReplay.get()) {
                            waitingForReplay.set(true);
                            broadcast("La manche est terminée. Voulez-vous rejouer ? (OUI ou QUIT)");
                            continue;
                        }

                        String word = line.substring(4).trim().toUpperCase();

                        if (word.length() != WORD_LENGTH) {
                            sendMessage("INVALID wrong_length (longueur = 5)");
                            sendMessage("Utilisez : TRY <mot> ou QUIT");
                            continue;
                        }
                        if (!word.chars().allMatch(Character::isLetter)) {
                            sendMessage("INVALID bad_chars (lettres uniquement)");
                            continue;
                        }

                        if (word.equals(currentSecret)) {
                            if (victoryAnnounced.compareAndSet(false, true)) {
                                broadcast("VICTORY " + clientId + " ANSWER " + currentSecret);
                                broadcast("BROADCAST [" + clientId + "] a trouvé le mot !");
                                System.out.println("[" + clientId + "] a gagné ! Mot : " + currentSecret);
                                roundActive = false;
                                waitingForReplay.set(true);
                                broadcast("La manche est terminée. Voulez-vous rejouer ? (OUI ou QUIT)");
                            }
                        } else {
                            String feedback = computeFeedbackSummary(currentSecret, word);
                            sendMessage("FEEDBACK :\n" + feedback);
                            sendMessage("Utilisez : TRY <mot> ou QUIT");
                        }
                    } else {
                        sendMessage("- Utilisez : TRY <mot> ou QUIT !!!");
                    }
                }
            } catch (IOException ignored) {
            } finally {
                cleanup();
            }
        }

        private String computeFeedbackSummary(String secret, String guess) {
            int n = secret.length();
            boolean[] usedSecret = new boolean[n];
            boolean[] usedGuess = new boolean[n];
            String[] result = new String[n];

            for (int i = 0; i < n; i++) {
                if (guess.charAt(i) == secret.charAt(i)) {
                    usedSecret[i] = usedGuess[i] = true;
                    result[i] = "OK_POS";
                }
            }

            for (int i = 0; i < n; i++) {
                if (usedGuess[i]) continue;
                char c = guess.charAt(i);
                boolean found = false;
                for (int j = 0; j < n; j++) {
                    if (!usedSecret[j] && secret.charAt(j) == c) {
                        usedSecret[j] = true;
                        found = true;
                        break;
                    }
                }
                result[i] = found ? "OK_MIS" : "WRONG";
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < n; i++) {
                sb.append("POSITION ").append(i + 1)
                        .append(": ").append(result[i]).append("\n");
            }

            return sb.toString().trim();
        }

        private void cleanup() {
            try {
                if (clientId != null) {
                    broadcast("BROADCAST 🔴 [" + clientId + "] s'est déconnecté.");
                    System.out.println("🔴 [" + clientId + "] déconnecté.");
                }
                socket.close();
            } catch (IOException ignored) {}
            clients.remove(this);
        }
    }
}