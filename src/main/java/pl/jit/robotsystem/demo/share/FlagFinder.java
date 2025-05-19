package pl.jit.robotsystem.demo.share;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FlagFinder {

    private FlagFinder() {
        // private constructor to prevent instantiation
    }

    public static boolean containsFlag(String string){
        if (string == null || string.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile("\\{\\{FLG:([^}]*)}}");
        Matcher matcher = pattern.matcher(string);
        if (!matcher.find()) {
            return false;
        }
        matcher.reset();
        while (matcher.find()) {
            System.out.println("//////////////////////");
            System.out.println("/// Znaleziono flagę: " + matcher.group(0));
            System.out.println("//////////////////////");
        }
        return true;
    }
}
