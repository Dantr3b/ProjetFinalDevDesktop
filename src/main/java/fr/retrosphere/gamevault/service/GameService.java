package fr.retrosphere.gamevault.service;

import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.repository.GameRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GameService {
    private final GameRepository repository = new GameRepository();

    public List<Game> allGames() {
        return repository.findAll();
    }

    public List<Game> search(String query, String platform, String sort) {
        String normalized = normalize(query);
        return allGames().stream()
                .filter(game -> normalized.isBlank() || searchableText(game).contains(normalized))
                .filter(game -> platform == null || platform.equals("All Platforms") || platform.equals(game.getPlatform()))
                .sorted(sortComparator(sort))
                .toList();
    }

    public Game save(Game game) {
        if (game == null) {
            throw new GameValidationException("Aucun jeu a enregistrer.");
        }
        validate(game);
        if (game.getAddedAt() == null) {
            game.setAddedAt(java.time.LocalDateTime.now());
        }
        return repository.save(game);
    }

    public void delete(Game game) {
        if (game != null && game.getId() != null) {
            repository.delete(game);
        }
    }

    private void validate(Game game) {
        require(game.getTitle(), "Le titre est obligatoire.");
        require(game.getDeveloper(), "Le developpeur est obligatoire.");
        require(game.getPublisher(), "L'editeur est obligatoire.");
        require(game.getPlatform(), "La plateforme est obligatoire.");

        int currentYear = Year.now().getValue() + 2;
        if (game.getReleaseYear() < 1970 || game.getReleaseYear() > currentYear) {
            throw new GameValidationException("L'annee de sortie doit etre comprise entre 1970 et " + currentYear + ".");
        }
        if (game.getRating() < 0 || game.getRating() > 10) {
            throw new GameValidationException("La note doit etre comprise entre 0 et 10.");
        }
        validateCoverPath(game.getCoverPath());
    }

    private void validateCoverPath(String coverPath) {
        if (coverPath == null || coverPath.isBlank()) {
            return;
        }
        String lower = coverPath.toLowerCase(Locale.ROOT);
        if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif"))) {
            throw new GameValidationException("L'image doit etre au format JPG, PNG ou GIF.");
        }
        // Covers are stored as local files; checking the path here avoids broken image previews later.
        if (!Files.exists(Path.of(coverPath))) {
            throw new GameValidationException("Le fichier image selectionne est introuvable.");
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new GameValidationException(message);
        }
    }

    private String searchableText(Game game) {
        return normalize(game.getTitle() + " " + game.getDeveloper() + " " + game.getPublisher()
                + " " + game.getPlatform() + " " + game.getGenre() + " " + game.getStatus());
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    public List<Game> favorites() {
        return allGames().stream().filter(Game::isFavorite).toList();
    }

    public void toggleFavorite(Game game) {
        if (game == null || game.getId() == null) {
            throw new GameValidationException("Selectionnez un jeu valide avant de modifier les favoris.");
        }
        if (!game.isFavorite() && favorites().size() >= 5) {
            throw new GameValidationException("Vous ne pouvez pas avoir plus de 5 jeux en favoris.");
        }
        game.setFavorite(!game.isFavorite());
        repository.save(game);
    }

    private Comparator<Game> sortComparator(String sort) {
        if ("Title".equals(sort) || "Titre".equals(sort)) {
            return Comparator.comparing(Game::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
        if ("Rating".equals(sort) || "Note".equals(sort)) {
            return Comparator.comparingDouble(Game::getRating).reversed();
        }
        if ("Release Year".equals(sort) || "Annee de sortie".equals(sort)) {
            return Comparator.comparingInt(Game::getReleaseYear).reversed();
        }
        return Comparator.comparing(Game::getAddedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
