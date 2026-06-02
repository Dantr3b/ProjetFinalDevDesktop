package fr.retrosphere.gamevault.service;

import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.repository.GameRepository;

import java.util.List;

public class GameSeeder {
    private final GameRepository repository = new GameRepository();

    public void seedIfEmpty() {
        if (repository.count() > 0) {
            return;
        }
        List<Game> games = List.of(
                new Game("Elden Ring", "FromSoftware", "Bandai Namco", 2022, "PS5", "Action RPG", "Finished", 9.6,
                        "Un monde ouvert exigeant, mémorable pour sa direction artistique et sa liberté d'exploration.", ""),
                new Game("Cyberpunk 2077", "CD Projekt RED", "CD Projekt", 2020, "PC", "Open World", "Owned", 8.9,
                        "RPG futuriste dans Night City, enrichi par une narration dense et une ambiance néon.", ""),
                new Game("Chrono Trigger", "Square", "Square", 1995, "Retro", "JRPG", "Finished", 9.8,
                        "Un classique du jeu de rôle japonais, conservé comme pièce majeure de la collection rétro.", ""),
                new Game("Zelda: Tears of the Kingdom", "Nintendo EPD", "Nintendo", 2023, "Switch", "Adventure", "Wishlist", 10.0,
                        "Aventure créative centrée sur l'exploration, la construction et la découverte.", ""),
                new Game("Forza Horizon 5", "Playground Games", "Xbox Game Studios", 2021, "Xbox", "Racing", "Owned", 9.2,
                        "Une vitrine automobile généreuse, idéale pour représenter les jeux de course modernes.", ""),
                new Game("Starfield", "Bethesda Game Studios", "Bethesda Softworks", 2023, "PC", "RPG", "Wishlist", 8.5,
                        "Grande fresque spatiale ajoutée au suivi des jeux à acquérir.", ""),
                new Game("Bloodborne", "FromSoftware", "Sony Interactive Entertainment", 2015, "PS5", "Soulslike", "Finished", 9.5,
                        "Action RPG gothique, reconnu pour son atmosphère et son rythme nerveux.", ""),
                new Game("Ori & Blind Forest", "Moon Studios", "Microsoft Studios", 2015, "Retro", "Platformer", "Owned", 9.0,
                        "Plateforme poétique et précis, apprécié pour sa direction artistique et sa musique.", "")
        );
        games.forEach(repository::save);
    }
}
