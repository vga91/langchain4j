package dev.langchain4j.store.embedding.neo4j;

import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThan;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsNotIn;
import dev.langchain4j.store.embedding.filter.logical.And;
import dev.langchain4j.store.embedding.filter.logical.Not;
import dev.langchain4j.store.embedding.filter.logical.Or;
import lombok.Getter;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Neo4jFilterMapper {

    public class IncrementalKeyMap {
        @Getter
        private final Map<String, Object> map = new ConcurrentHashMap<>();
        private int counter = 1;

        // Method to add a value with an auto-incremented key
        public String put(Object value) {
            String key = "param_" + counter++;
            map.put(key, value);
            return key;
        }

    }


    public Neo4jFilterMapper() {}

    private int counter = 1;
    final IncrementalKeyMap map = new IncrementalKeyMap();

    AbstractMap.SimpleEntry<String, Map> map(Filter filter) {
//    String map(Filter filter) {
        final String stringMapPair = getMapping(filter);
        return new AbstractMap.SimpleEntry<>(stringMapPair, map.getMap());
    }

    private String getMapping(Filter filter) {
        if (filter instanceof IsEqualTo item) {
            return getString(item.key(), "=", item.comparisonValue());
        } else if (filter instanceof IsNotEqualTo item) {
            return getString(item.key(), "<>", item.comparisonValue());
        } else if (filter instanceof IsGreaterThan item) {
            return getString(item.key(), ">", item.comparisonValue());
        } else if (filter instanceof IsGreaterThanOrEqualTo item) {
            return getString(item.key(), ">=", item.comparisonValue());
        } else if (filter instanceof IsLessThan item) {
            return getString(item.key(), "<", item.comparisonValue());
        } else if (filter instanceof IsLessThanOrEqualTo item) {
            return getString(item.key(), "<=", item.comparisonValue());
        } else if (filter instanceof IsIn item) {
            return mapIn(item);
        } else if (filter instanceof IsNotIn item) {
            return mapNotIn(item);
        } else if (filter instanceof And item) {
            return mapAnd(item);
        } else if (filter instanceof Not item) {
            return mapNot(item);
        } else if (filter instanceof Or item) {
            return mapOr(item);
        } else {
            throw new UnsupportedOperationException("Unsupported filter type: " + filter.getClass().getName());
        }
    }

    private String getString(String key, String operator, Object value) {
        final String param = map.put(value);

        return "n.`%s` %s $%s".formatted(
                key, operator, param
        );
    }

    private String mapEqual(IsEqualTo filter) {
        return getString(filter.key(), "=", filter.comparisonValue());
    }

    private String mapNotEqual(IsNotEqualTo filter) {
        return "%s <> %s".formatted(
                filter.key(), filter.comparisonValue()
        );
    }

    private String mapGreaterThan(IsGreaterThan filter) {
        return "%s > %s".formatted(
                filter.key(), filter.comparisonValue()
        );
    }

    private String mapGreaterThanOrEqual(IsGreaterThanOrEqualTo filter) {
        return "%s >= %s".formatted(
                filter.key(), filter.comparisonValue()
        );
    }

    private String mapLessThan(IsLessThan filter) {
        return "%s < %s".formatted(
                filter.key(), filter.comparisonValue()
        );
    }

    private String mapLessThanOrEqual(IsLessThanOrEqualTo filter) {
        return "%s <= %s".formatted(
                filter.key(), filter.comparisonValue()
        );
    }

    public String mapIn(IsIn filter) {
        // todo - evaluate, square brackets??

        return getString(filter.key(), "IN", filter.comparisonValues());
    }

//    private static String getStringIn(String key, Collection<?> objects) {
//        return "%s IN %s".formatted(
//                key, objects
//        );
//    }

    public String mapNotIn(IsNotIn filter) {
        return "NOT " + getString(filter.key(), "IN", filter.comparisonValues());
        //return "NOT (%s IN [%s])".formatted(
        //        filter.key(), filter.comparisonValues()
        //);
    }

    private String mapAnd(And filter) {
        return "(%s) AND (%s)".formatted(
                getMapping(filter.left()), getMapping(filter.right())
        );
    }

    private String mapOr(Or filter) {
        return "(%s) OR (%s)".formatted(
                getMapping(filter.left()), getMapping(filter.right())
        );
    }

    private String mapNot(Not filter) {
        return "NOT (%s)".formatted(
                getMapping(filter.expression())
        );
        //Filter expression = not.expression();
        // TODO
        //return map(expression);
    }
}
