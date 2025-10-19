# Word-Guess-Game


## Équipe
- LAARAICH Lina
- BOUMOUS Oumayma
- ELKANICH Fatima Ezzahra
- ESSAFI Fatima Ezzahrae
- ELMOUSSAOUI Safae
- BOULANOUAR Btissam
- OUTLIOUA Zakia
- BOURRAT Yosra
- NACIRI Fatima Zahra
- LAASSAL Asmaa
- WARDY Zakia
- AYOUB Botaina
- SRIJA Fatima Zahra
- OUYAHIA Salma
- DARIF Salma
- MEKTANE Nadia
- EL FARISSI Noura 
- ERRADY Hanane
- BOULANOUAR BTISSAM

## Structure du Projet

```
Word-guess-game/
├── Server/
│   ├── src/GuessServer.java
│   └── Server.jar
├── Client/
    ├── src/GuessClient.java
    └── Client.jar

```
## Serveur (AWS EC2)

- **IP** : `3.95.228.78`
- **Port** : `8888`
- **Java** : OpenJDK 21



### Commandes

- `TRY <mot>` : Proposer un mot de 5 lettres
- `QUIT` : Quitter le jeu

### Feedback

- `OK_POS` : Bonne lettre, bonne position
- `OK_MIS` : Bonne lettre, mauvaise position
- `WRONG` : Lettre absente
  
### Technologies

- Java 21
- TCP/IP
- AWS EC2
