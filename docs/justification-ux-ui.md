# Justification UX/UI

## Intention générale

GameVault vise un usage quotidien par des collectionneurs. L'interface privilégie donc une navigation stable, une lecture rapide des informations importantes et des actions faciles à retrouver. Le thème sombre reprend la maquette Figma : il donne une identité de coffre d'archives numérique tout en gardant un bon contraste sur les textes principaux.

## Organisation de l'écran principal

La navigation est placée dans une barre latérale fixe. Ce choix évite de déplacer les accès principaux lorsque l'utilisateur consulte une longue collection. Les entrées `My Collection`, `Statistics` et `Settings` restent visibles en permanence, ce qui rend le parcours prévisible.

La collection est affichée en grille de cartes. Chaque carte met en avant la jaquette ou un visuel de remplacement, puis les informations les plus utiles pour scanner rapidement la bibliothèque : titre, plateforme, genre, statut et note. Le clic sur une carte mène au détail, ce qui garde l'écran principal léger.

## Recherche, filtre et tri

La recherche est placée dans la barre supérieure, car c'est l'action la plus fréquente quand la collection grandit. Les filtres de plateforme sont directement au-dessus de la grille pour rester proches des résultats qu'ils modifient. Le tri est séparé en bas de l'interface afin de conserver un contrôle global sans surcharger la zone de cartes.

## Ajout et modification d'un jeu

Le formulaire reprend une logique de saisie guidée. Les champs d'identification essentiels sont en haut : titre, développeur, éditeur, année et plateforme. Les informations plus descriptives, comme la note et la description, viennent ensuite. La zone de jaquette est séparée à droite pour rappeler que l'image est importante mais optionnelle.

Les boutons `Cancel` et `Save to Vault` sont en bas du formulaire, dans le sens naturel de lecture. L'action principale est visuellement plus forte pour aider l'utilisateur à finaliser l'ajout.

## Prévention des erreurs

Les champs obligatoires sont validés avant sauvegarde. Une année incohérente, une note hors limites ou une image au mauvais format génère un message clair dans le formulaire. La suppression d'un jeu demande une confirmation, car il s'agit d'une action destructrice.

## Ecran détail

Le détail d'un jeu donne plus d'espace au visuel, au titre et aux métadonnées. Les actions sensibles sont regroupées : modification et suppression. La description longue est séparée sous le bloc principal afin de ne pas ralentir la lecture des informations essentielles.

## Statistiques et paramètres

Les statistiques utilisent des cartes synthétiques et des graphiques simples. L'objectif n'est pas l'analyse avancée, mais une compréhension immédiate de la collection : nombre de jeux, plateformes, moyenne des notes et meilleurs titres.

Les paramètres sont organisés par sections : général, apparence, base de données. Cette séparation permet d'ajouter de futurs réglages sans transformer l'écran en liste confuse.

## Cohérence avec Figma

Les choix visuels principaux de la maquette ont été conservés : fond bleu nuit, panneaux translucides, violet pour les actions principales, cyan pour les accents, cartes arrondies et navigation latérale. L'implémentation JavaFX adapte ces choix à une application desktop maintenable, sans dépendre des images temporaires générées par Figma.
