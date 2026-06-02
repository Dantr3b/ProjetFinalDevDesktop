package fr.retrosphere.gamevault.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SteamGameApiClient {
    private static final Pattern APP_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
    private static final Pattern NAME_PATTERN = Pattern.compile("\"name\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern SHORT_DESCRIPTION_PATTERN = Pattern.compile("\"short_description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern HEADER_IMAGE_PATTERN = Pattern.compile("\"header_image\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern DEVELOPERS_PATTERN = Pattern.compile("\"developers\"\\s*:\\s*\\[\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern PUBLISHERS_PATTERN = Pattern.compile("\"publishers\"\\s*:\\s*\\[\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern RELEASE_DATE_PATTERN = Pattern.compile("\"release_date\"\\s*:\\s*\\{[^}]*\"date\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern GENRES_BLOCK_PATTERN = Pattern.compile("\"genres\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL);
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("\"description\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(19|20)\\d{2}");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GameApiResult fetchGame(String query) throws IOException, InterruptedException {
        String appId = findSteamAppId(query)
                .orElseThrow(() -> new IOException("Aucun jeu trouve sur Steam pour ce titre."));
        String detailsJson = get("https://store.steampowered.com/api/appdetails?appids="
                + appId + "&l=english");

        if (!detailsJson.contains("\"success\":true")) {
            throw new IOException("Steam n'a pas renvoye de fiche exploitable pour ce jeu.");
        }

        String title = extract(NAME_PATTERN, detailsJson).orElse(query);
        String developer = extract(DEVELOPERS_PATTERN, detailsJson).orElse("");
        String publisher = extract(PUBLISHERS_PATTERN, detailsJson).orElse("");
        String releaseDate = extract(RELEASE_DATE_PATTERN, detailsJson).orElse("");
        String releaseYear = extractYear(releaseDate).orElse("");
        String genre = extractGenre(detailsJson).orElse("");
        String description = extract(SHORT_DESCRIPTION_PATTERN, detailsJson).orElse("");
        String headerImage = extract(HEADER_IMAGE_PATTERN, detailsJson).orElse("");
        String localCoverPath = downloadCover(headerImage, title, appId).orElse("");

        return new GameApiResult(title, developer, publisher, releaseYear, genre, description, localCoverPath);
    }

    private Optional<String> findSteamAppId(String query) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json = get("https://store.steampowered.com/api/storesearch/?term="
                + encodedQuery + "&l=english&cc=US");
        return extract(APP_ID_PATTERN, json);
    }

    private String get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "GameVault/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Erreur API Steam: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private Optional<String> downloadCover(String imageUrl, String title, String appId) throws IOException, InterruptedException {
        if (imageUrl == null || imageUrl.isBlank()) {
            return Optional.empty();
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .header("User-Agent", "GameVault/1.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length == 0) {
            return Optional.empty();
        }

        Path coverDirectory = Path.of("data", "covers");
        Files.createDirectories(coverDirectory);
        Path coverPath = coverDirectory.resolve(safeFileName(title) + "-" + appId + ".jpg");
        Files.write(coverPath, response.body());
        return Optional.of(coverPath.toAbsolutePath().toString());
    }

    private Optional<String> extractGenre(String json) {
        Matcher blockMatcher = GENRES_BLOCK_PATTERN.matcher(json);
        if (!blockMatcher.find()) {
            return Optional.empty();
        }
        return extract(DESCRIPTION_PATTERN, blockMatcher.group(1));
    }

    private Optional<String> extractYear(String releaseDate) {
        Matcher matcher = YEAR_PATTERN.matcher(releaseDate);
        return matcher.find() ? Optional.of(matcher.group()) : Optional.empty();
    }

    private Optional<String> extract(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? Optional.of(unescapeJson(matcher.group(1)).trim()) : Optional.empty();
    }

    private String unescapeJson(String value) {
        String basicValue = value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\/", "/")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ");
        Matcher unicodeMatcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(basicValue);
        StringBuilder decoded = new StringBuilder();
        while (unicodeMatcher.find()) {
            int codePoint = Integer.parseInt(unicodeMatcher.group(1), 16);
            unicodeMatcher.appendReplacement(decoded, Matcher.quoteReplacement(String.valueOf((char) codePoint)));
        }
        unicodeMatcher.appendTail(decoded);
        return decoded.toString();
    }

    private String safeFileName(String value) {
        String cleaned = value == null ? "steam-cover" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return cleaned.isBlank() ? "steam-cover" : cleaned;
    }

    public record GameApiResult(
            String title,
            String developer,
            String publisher,
            String releaseYear,
            String genre,
            String description,
            String coverPath
    ) {
    }
}
