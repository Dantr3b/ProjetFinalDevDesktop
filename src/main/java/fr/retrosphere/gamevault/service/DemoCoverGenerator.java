package fr.retrosphere.gamevault.service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DemoCoverGenerator {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 800;

    public String coverFor(String title, String platform, String genre) {
        try {
            Path directory = Path.of("data", "covers", "demo");
            Files.createDirectories(directory);
            Path coverPath = directory.resolve(safeFileName(title) + ".png");
            if (!Files.exists(coverPath)) {
                ImageIO.write(renderCover(title, platform, genre), "png", coverPath.toFile());
            }
            return coverPath.toAbsolutePath().toString();
        } catch (IOException exception) {
            return "";
        }
    }

    private BufferedImage renderCover(String title, String platform, String genre) {
        CoverPalette palette = paletteFor(title);
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintBackground(graphics, palette);
        paintFrame(graphics, palette);
        paintTitle(graphics, title);
        paintMetadata(graphics, platform, genre, palette);

        graphics.dispose();
        return image;
    }

    private void paintBackground(Graphics2D graphics, CoverPalette palette) {
        for (int y = 0; y < HEIGHT; y++) {
            float ratio = y / (float) HEIGHT;
            graphics.setColor(blend(palette.top(), palette.bottom(), ratio));
            graphics.drawLine(0, y, WIDTH, y);
        }

        graphics.setColor(withAlpha(palette.accent(), 56));
        graphics.fillOval(-110, 70, 320, 320);
        graphics.fillOval(370, 430, 310, 310);

        graphics.setColor(withAlpha(Color.WHITE, 28));
        for (int x = -120; x < WIDTH; x += 96) {
            graphics.setStroke(new BasicStroke(3f));
            graphics.drawLine(x, 0, x + 340, HEIGHT);
        }
    }

    private void paintFrame(Graphics2D graphics, CoverPalette palette) {
        graphics.setColor(withAlpha(Color.BLACK, 80));
        graphics.fill(new RoundRectangle2D.Double(44, 44, WIDTH - 88, HEIGHT - 88, 36, 36));
        graphics.setColor(withAlpha(Color.WHITE, 64));
        graphics.setStroke(new BasicStroke(3f));
        graphics.draw(new RoundRectangle2D.Double(44, 44, WIDTH - 88, HEIGHT - 88, 36, 36));

        graphics.setColor(withAlpha(palette.accent(), 190));
        graphics.fillRoundRect(84, 112, WIDTH - 168, 12, 14, 14);
        graphics.fillRoundRect(84, HEIGHT - 136, WIDTH - 168, 12, 14, 14);
    }

    private void paintTitle(Graphics2D graphics, String title) {
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 58));
        List<String> lines = wrap(title.toUpperCase(), graphics.getFontMetrics(), WIDTH - 150);
        int lineHeight = 68;
        int y = 300 - (lines.size() * lineHeight / 2);
        for (String line : lines) {
            drawCentered(graphics, line, y);
            y += lineHeight;
        }
    }

    private void paintMetadata(Graphics2D graphics, String platform, String genre, CoverPalette palette) {
        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        graphics.setColor(withAlpha(Color.BLACK, 110));
        graphics.fillRoundRect(92, 590, WIDTH - 184, 86, 20, 20);

        graphics.setColor(palette.accent());
        drawCentered(graphics, platform == null ? "GAMEVAULT" : platform.toUpperCase(), 625);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 22));
        graphics.setColor(withAlpha(Color.WHITE, 210));
        drawCentered(graphics, genre == null ? "Demo archive cover" : genre, 660);
    }

    private List<String> wrap(String value, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : value.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (metrics.stringWidth(candidate) > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private void drawCentered(Graphics2D graphics, String text, int baselineY) {
        FontMetrics metrics = graphics.getFontMetrics();
        int x = (WIDTH - metrics.stringWidth(text)) / 2;
        graphics.drawString(text, x, baselineY);
    }

    private CoverPalette paletteFor(String title) {
        int hash = Math.abs(title == null ? 0 : title.hashCode());
        CoverPalette[] palettes = {
                new CoverPalette(new Color(25, 33, 61), new Color(89, 45, 142), new Color(208, 188, 255)),
                new CoverPalette(new Color(10, 42, 62), new Color(80, 18, 76), new Color(0, 229, 255)),
                new CoverPalette(new Color(46, 31, 20), new Color(109, 65, 25), new Color(255, 196, 87)),
                new CoverPalette(new Color(15, 52, 38), new Color(19, 91, 82), new Color(111, 255, 196)),
                new CoverPalette(new Color(63, 22, 37), new Color(20, 27, 49), new Color(255, 113, 141))
        };
        return palettes[hash % palettes.length];
    }

    private Color blend(Color first, Color second, float ratio) {
        int red = Math.round(first.getRed() + (second.getRed() - first.getRed()) * ratio);
        int green = Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * ratio);
        int blue = Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * ratio);
        return new Color(red, green, blue);
    }

    private Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private String safeFileName(String value) {
        String cleaned = value == null ? "demo-cover" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return cleaned.isBlank() ? "demo-cover" : cleaned;
    }

    private record CoverPalette(Color top, Color bottom, Color accent) {
    }
}
