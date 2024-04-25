package fr.kysio.reminderbot.utils;

import java.util.List;

public class QuoteRandomizer {

    private static final List<String> quotes = List.of(
            "\"Le temps est un grand maitre, dit-on. Le malheur est qu'il tue ses eleves.\" - Hector Berlioz",
            "\"Le temps est un fleuve sans rivage.\" - Marc Aurele",
            "\"Le temps est ce que nous avons de plus precieux et ce qui nous echappe le plus.\" - Leonard de Vinci",
            "\"Le temps est un grand livre. Quand on tourne la derniere page, on est bien souvent etonne de voir combien on a aime l'auteur.\" - Marcel Achard",
            "\"Le temps est un vetement dont la nature nous habille.\" - Seneque",
            "\"Le temps passe, et chaque jour passé ne revient pas.\" - Virgile",
            "\"Le temps est un puissant facteur de la croissance de l'amour.\" - Honore de Balzac",
            "\"Le temps est une illusion.\" - Albert Einstein",
            "\"Il est trop court le temps ou l'on aime, et il est long le temps ou l'on attend.\" - Jean de La Fontaine",
            "\"Le temps ne respecte pas ce qui se fait sans lui.\" - Baltasar Gracian"
    );

    public static String getRandomQuote() {
        return quotes.get((int) (Math.random() * quotes.size()));
    }

}
