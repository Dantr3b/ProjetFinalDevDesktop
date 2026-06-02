package fr.retrosphere.gamevault.service;

import fr.retrosphere.gamevault.model.Game;
import fr.retrosphere.gamevault.repository.GameRepository;

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
        require(game.getDeveloper(), "Le développeur est obligatoire.");
        require(game.getPublisher(), "L'éditeur est obligatoire.");
        require(game.getPlatform(), "La plateforme est obligatoire.");
        int currentYear = Year.now().getValue() + 2;
        if (game.getReleaseYear() < 1970 || game.getReleaseYear() > currentYear) {
            throw new GameValidationException("L'année de sortie doit être comprise entre 1970 et " + currentYear + ".");
        }
        if (game.getRating() < 0 || game.getRating() > 10) {
            throw new GameValidationException("La note doit être comprise entre 0 et 10.");
        }
        if (game.getCoverPath() != null && !game.getCoverPath().isBlank()) {
            String lower = game.getCoverPath().toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif"))) {
                throw new GameValidationException("L'image doit être au format JPG, PNG ou GIF.");
            }
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
