package com.neueda.instrumentservice.services.assistant;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.dtos.ai.AssistantSource;
import com.neueda.instrumentservice.dtos.ai.ToolResult;

@Component
public class ReplyVerifier {

    static final BigDecimal LARGEST_UNCHECKED_WHOLE_NUMBER = BigDecimal.valueOf(31);
    static final List<String> NAMES_WITH_NUMBERS = List.of("S&P 500", "Nasdaq-100", "Nasdaq 100", "Russell 2000");

    private static final Pattern REPLY_NUMBER = Pattern.compile(
            "(?<![A-Za-z0-9.,])(\\$\\s?)?(\\d{1,3}(?:,\\d{3})+|\\d+)(\\.\\d+)?(?:\\s?(%))?");
    private static final Pattern ANY_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern URL = Pattern.compile("https?://[^\\s)\\]>\"']+");

    private final ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    public record Verification(boolean passed, List<String> problems) {}

    public Verification verify(String reply, List<ToolResult> results, List<String> trustedTexts) {
        List<String> problems = new ArrayList<>();
        if (reply == null || reply.isBlank()) {
            return new Verification(false, List.of("The reply was empty"));
        }
        Set<BigDecimal> allowed = allowedNumbers(results, trustedTexts);
        for (String number : unsupportedNumbers(reply, allowed)) {
            problems.add("The figure " + number + " does not appear in the data");
        }
        Set<String> allowedUrls = new HashSet<>();
        results.forEach(r -> r.sources().stream().map(AssistantSource::url).filter(Objects::nonNull)
                .forEach(allowedUrls::add));
        Matcher url = URL.matcher(reply);
        while (url.find()) {
            String link = url.group().replaceAll("[.,;:!?]+$", "");
            if (!allowedUrls.contains(link)) {
                problems.add("The link " + link + " was not returned by the news data");
            }
        }
        return new Verification(problems.isEmpty(), List.copyOf(problems));
    }

    Set<String> unsupportedNumbers(String reply, Set<BigDecimal> allowed) {
        String text = reply;
        for (String name : NAMES_WITH_NUMBERS) {
            text = text.replace(name, " ");
        }
        Set<String> unsupported = new LinkedHashSet<>();
        Matcher m = REPLY_NUMBER.matcher(text);
        while (m.find()) {
            String digits = m.group(2).replace(",", "") + (m.group(3) == null ? "" : m.group(3));
            BigDecimal value = new BigDecimal(digits);
            boolean looksLikeAFigure = m.group(1) != null || m.group(3) != null || m.group(4) != null
                    || value.compareTo(LARGEST_UNCHECKED_WHOLE_NUMBER) > 0;
            if (looksLikeAFigure && !matchesAny(value, allowed)) {
                unsupported.add(m.group().strip());
            }
        }
        return unsupported;
    }

    static boolean matchesAny(BigDecimal quoted, Set<BigDecimal> allowed) {
        BigDecimal tolerance = BigDecimal.ONE.movePointLeft(Math.max(quoted.scale(), 0)).divide(BigDecimal.valueOf(2));
        for (BigDecimal candidate : allowed) {
            if (candidate.subtract(quoted).abs().compareTo(tolerance) <= 0) {
                return true;
            }
        }
        return false;
    }

    Set<BigDecimal> allowedNumbers(List<ToolResult> results, List<String> trustedTexts) {
        Set<BigDecimal> allowed = new HashSet<>();
        for (ToolResult result : results) {
            try {
                collect(mapper.readTree(result.content()), allowed);
            } catch (JsonProcessingException ex) {
                addNumbersIn(result.content(), allowed);
            }
        }
        trustedTexts.forEach(text -> addNumbersIn(text, allowed));
        return allowed;
    }

    private static void collect(JsonNode node, Set<BigDecimal> allowed) {
        if (node.isNumber()) {
            allowed.add(node.decimalValue().abs());
        } else if (node.isTextual()) {
            addNumbersIn(node.asText(), allowed);
        } else if (node.isContainerNode()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                addNumbersIn(field.getKey(), allowed);
                collect(field.getValue(), allowed);
            }
            if (node.isArray()) {
                node.forEach(child -> collect(child, allowed));
            }
        }
    }

    private static void addNumbersIn(String text, Set<BigDecimal> allowed) {
        if (text == null) {
            return;
        }
        Matcher m = ANY_NUMBER.matcher(text.replaceAll("(?<=\\d),(?=\\d{3})", ""));
        while (m.find()) {
            allowed.add(new BigDecimal(m.group()));
        }
    }
}
