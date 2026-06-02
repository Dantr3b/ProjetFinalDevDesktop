# GameVault

GameVault est une application desktop JavaFX conçue pour l'association RetroSphere. Elle permet de gérer une collection de jeux vidéo avec recherche, filtres, tri, ajout, modification, suppression, détail de jeu, statistiques et paramètres.

## Fonctionnalités

- Consultation de la collection sous forme de cartes visuelles.
- Recherche par titre, développeur, éditeur, plateforme, genre ou statut.
- Filtre par plateforme et tri par ajout récent, titre, note ou année de sortie.
- Ajout et modification d'un jeu avec validation des champs obligatoires.
- Recuperation automatique des informations d'un jeu via l'API publique Steam.
- Suppression avec confirmation utilisateur.
- Persistance SQLite via Hibernate.
- Données de démonstration automatiquement créées au premier lancement.
- Ecrans supplémentaires : détail de jeu, statistiques, paramètres et profil.

## Stack technique

- Java 21 ou plus récent
- JavaFX 23
- FXML + contrôleurs JavaFX
- Maven
- Hibernate ORM
- SQLite

## Lancement

1. Installer Java 21+ et Maven.
2. Cloner le dépôt.
3. Depuis la racine du projet, lancer :

```powershell
mvn clean javafx:run
```

La base SQLite est créée automatiquement dans `data/gamevault.db`.

## Compilation

```powershell
mvn clean package
```

## Configuration

Les paramètres principaux sont dans :

```text
src/main/resources/application.properties
```

Le chemin de la base peut être modifié via :

```properties
database.url=jdbc:sqlite:data/gamevault.db
```

## Maquette Figma

- Ma Bibliothèque : https://www.figma.com/design/ec8xxk2sc2PyamQ9Ai7NDa/ProjetDevDesktop?node-id=0-152&m=dev
- Ajouter un jeu : https://www.figma.com/design/ec8xxk2sc2PyamQ9Ai7NDa/ProjetDevDesktop?node-id=0-3&m=dev
- Détail de jeu : https://www.figma.com/design/ec8xxk2sc2PyamQ9Ai7NDa/ProjetDevDesktop?node-id=0-401&m=dev
- Statistiques : https://www.figma.com/design/ec8xxk2sc2PyamQ9Ai7NDa/ProjetDevDesktop?node-id=0-614&m=dev
- Paramètres : https://www.figma.com/design/ec8xxk2sc2PyamQ9Ai7NDa/ProjetDevDesktop?node-id=1-13&m=dev
- Profil utilisateur : https://www.figma.com/design/ec8xxk2sc2PyamQ9Ai7NDa/ProjetDevDesktop?node-id=1-195&m=dev

## Architecture

```text
src/main/java/fr/retrosphere/gamevault
├── config          Configuration applicative
├── controller      Contrôleurs JavaFX
├── model           Entités Hibernate
├── persistence     Initialisation Hibernate / SQLite
├── repository      Accès aux données
└── service         Validation, recherche, tri, données de démo
```

L'interface est décrite dans `src/main/resources/fxml`, tandis que le thème visuel se trouve dans `src/main/resources/styles/gamevault.css`.

## Robustesse

GameVault valide les données avant sauvegarde :

- titre, développeur, éditeur et plateforme obligatoires ;
- année cohérente ;
- note comprise entre 0 et 10 ;
- format d'image limité à JPG, PNG ou GIF ;
- confirmation avant suppression.

En cas d'erreur, l'application affiche un message utilisateur au lieu de s'arrêter brutalement.
