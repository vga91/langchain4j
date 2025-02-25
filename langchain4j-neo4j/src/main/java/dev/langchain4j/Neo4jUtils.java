package dev.langchain4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Neo4jUtils {

    private static final Pattern BACKTICKS_PATTERN = Pattern.compile("```(.*?)```", Pattern.MULTILINE | Pattern.DOTALL);

    /**
     * Get the text block wrapped by triple backticks
     */
    public static String getBacktickText(String cypherQuery) {
        Matcher matcher = BACKTICKS_PATTERN.matcher(cypherQuery);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return cypherQuery;
    }
}
