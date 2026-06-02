package fr.retrosphere.gamevault;

/**
 * Ce Launcher séparé est nécessaire pour contourner une restriction de Java 11+.
 * Si la classe principale étendue de Application (MainApp) est directement appelée
 * depuis un Fat JAR, Java va essayer de charger les modules JavaFX qui ne sont
 * pas dans le module path, ce qui provoque une erreur "JavaFX runtime components are missing".
 * 
 * En utilisant cette classe basique qui n'étend rien, on trompe le lanceur
 * et JavaFX se charge correctement depuis le classpath classique du JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
