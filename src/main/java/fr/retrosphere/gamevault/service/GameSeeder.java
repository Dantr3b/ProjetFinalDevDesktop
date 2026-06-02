package fr.retrosphere.gamevault.service;

import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.repository.GameRepository;

import java.util.List;

public class GameSeeder {
    private final GameRepository repository = new GameRepository();
    private final DemoCoverGenerator coverGenerator = new DemoCoverGenerator();

    public void seedIfEmpty() {
        if (repository.count() > 0) {
            ensureDemoCovers();
            return;
        }

        List<Game> games = List.of(
                new Game("Elden Ring", "FromSoftware", "Bandai Namco", 2022, "PS5", "Action RPG", "Finished", 9.6,
                        "Un monde ouvert exigeant, memorable pour sa direction artistique et sa liberte d'exploration.",
                        demoCover("Elden Ring", "PS5", "Action RPG")),
                new Game("Cyberpunk 2077", "CD Projekt RED", "CD Projekt", 2020, "PC", "Open World", "Owned", 8.9,
                        "RPG futuriste dans Night City, enrichi par une narration dense et une ambiance neon.",
                        demoCover("Cyberpunk 2077", "PC", "Open World")),
                new Game("Chrono Trigger", "Square", "Square", 1995, "Retro", "JRPG", "Finished", 9.8,
                        "Un classique du jeu de role japonais, conserve comme piece majeure de la collection retro.",
                        demoCover("Chrono Trigger", "Retro", "JRPG")),
                new Game("Zelda: Tears of the Kingdom", "Nintendo EPD", "Nintendo", 2023, "Switch", "Adventure", "Wishlist", 10.0,
                        "Aventure creative centree sur l'exploration, la construction et la decouverte.",
                        demoCover("Zelda: Tears of the Kingdom", "Switch", "Adventure")),
                new Game("Forza Horizon 5", "Playground Games", "Xbox Game Studios", 2021, "Xbox", "Racing", "Owned", 9.2,
                        "Une vitrine automobile genereuse, ideale pour representer les jeux de course modernes.",
                        demoCover("Forza Horizon 5", "Xbox", "Racing")),
                new Game("Starfield", "Bethesda Game Studios", "Bethesda Softworks", 2023, "PC", "RPG", "Wishlist", 8.5,
                        "Grande fresque spatiale ajoutee au suivi des jeux a acquerir.",
                        demoCover("Starfield", "PC", "RPG")),
                new Game("Bloodborne", "FromSoftware", "Sony Interactive Entertainment", 2015, "PS5", "Soulslike", "Finished", 9.5,
                        "Action RPG gothique, reconnu pour son atmosphere et son rythme nerveux.",
                        demoCover("Bloodborne", "PS5", "Soulslike")),
                new Game("Ori & Blind Forest", "Moon Studios", "Microsoft Studios", 2015, "Retro", "Platformer", "Owned", 9.0,
                        "Plateforme poetique et precis, apprecie pour sa direction artistique et sa musique.",
                        demoCover("Ori & Blind Forest", "Retro", "Platformer"))
        );
        games.forEach(repository::save);
    }

    private void ensureDemoCovers() {
        repository.findAll().stream()
                .filter(game -> game.getCoverPath() == null || game.getCoverPath().isBlank())
                .filter(game -> isDemoGame(game.getTitle()))
                .forEach(game -> {
                    game.setCoverPath(demoCover(game.getTitle(), game.getPlatform(), game.getGenre()));
                    repository.save(game);
                });
    }

    private boolean isDemoGame(String title) {
        return title != null && List.of(
                "Elden Ring",
                "Cyberpunk 2077",
                "Chrono Trigger",
                "Zelda: Tears of the Kingdom",
                "Forza Horizon 5",
                "Starfield",
                "Bloodborne",
                "Ori & Blind Forest"
        ).contains(title);
    }

    private String demoCover(String title, String platform, String genre) {
        return coverGenerator.coverFor(title, platform, genre);
    }
}
