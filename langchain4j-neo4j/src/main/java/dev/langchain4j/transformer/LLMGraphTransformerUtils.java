package dev.langchain4j.transformer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class LLMGraphTransformerUtils {

    public static final List<Map<String, String>> EXAMPLES_PROMPT = Arrays.asList(
            Map.of(
                    "text", "Adam is a software engineer in Microsoft since 2009, and last year he got an award as the Best Talent",
                    "head", "Adam",
                    "head_type", "Person",
                    "relation", "WORKS_FOR",
                    "tail", "Microsoft",
                    "tail_type", "Company"
            ),
            Map.of(
                    "text", "Adam is a software engineer in Microsoft since 2009, and last year he got an award as the Best Talent",
                    "head", "Adam",
                    "head_type", "Person",
                    "relation", "HAS_AWARD",
                    "tail", "Best Talent",
                    "tail_type", "Award"
            ),
            Map.of(
                    "text", "Microsoft is a tech company that provide several products such as Microsoft Word",
                    "head", "Microsoft Word",
                    "head_type", "Product",
                    "relation", "PRODUCED_BY",
                    "tail", "Microsoft",
                    "tail_type", "Company"
            ),
            Map.of(
                    "text", "Microsoft Word is a lightweight app that accessible offline",
                    "head", "Microsoft Word",
                    "head_type", "Product",
                    "relation", "HAS_CHARACTERISTIC",
                    "tail", "lightweight app",
                    "tail_type", "Characteristic"
            ),
            Map.of(
                    "text", "Microsoft Word is a lightweight app that accessible offline",
                    "head", "Microsoft Word",
                    "head_type", "Product",
                    "relation", "HAS_CHARACTERISTIC",
                    "tail", "accessible offline",
                    "tail_type", "Characteristic"
            )
    );
}
