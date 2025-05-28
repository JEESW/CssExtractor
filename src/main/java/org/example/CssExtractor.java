package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * CSS Extractor
 * <p>
 *     기존 style 태그 안에 있는 클래스를 각 요소에 인라인 스타일로 넣은 후,
 *     중복되는 인라인 스타일을 모아 class로 변환합니다.
 * </p>
 */

public class CssExtractor {
    public static void main(String[] args) throws IOException {
        // 1. HTML 파일 읽기
        File input = new File("FCR_HTML.htm");
        if (!input.exists()) {
            throw new FileNotFoundException("파일을 찾을 수 없습니다: " + input.getAbsolutePath());
        }
        Document doc = Jsoup.parse(input, "UTF-8");

        // 2. 기존 style 태그 파싱 후 각 클래스에 인라인 스타일 적용
        Element head = doc.head();
        Element existingStyle = head.select("style").first();
        Map<String, String> classToStyle = new HashMap<>();

        if (existingStyle != null) {
            String[] lines = existingStyle.html().split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith(".") && line.contains("{")) {
                    String className = line.substring(1, line.indexOf(" ")).trim();
                    String style = line.substring(line.indexOf("{") + 1, line.indexOf("}")).trim();
                    classToStyle.put(className, style);
                }
            }
        }

        Elements allElements = doc.body().getAllElements();
        for (Element el : allElements) {
            String classAttr = el.className();
            if (!classAttr.isEmpty()) {
                StringBuilder combinedStyle = new StringBuilder();
                for (String cls : classAttr.split(" ")) {
                    if (classToStyle.containsKey(cls)) {
                        combinedStyle.append(classToStyle.get(cls)).append(" ");
                    }
                }
                if (combinedStyle.length() > 0) {
                    el.attr("style", combinedStyle.toString().trim());
                }
                el.removeAttr("class");
            }
        }

        if (existingStyle != null) {
            existingStyle.remove();
        }

        // 3. 중복 인라인 스타일을 class로 묶기
        Map<String, String> styleMap = new LinkedHashMap<>();
        Map<String, Set<Element>> elementsByStyle = new HashMap<>();
        int styleCounter = 1;

        Elements elementsWithStyle = doc.body().select("[style]");
        for (Element element : elementsWithStyle) {
            String style = element.attr("style").trim();
            if (!style.isEmpty()) {
                String className = styleMap.get(style);
                if (className == null) {
                    className = "Extracted" + styleCounter++;
                    styleMap.put(style, className);
                    elementsByStyle.put(className, new HashSet<Element>());
                }

                elementsByStyle.get(className).add(element);
                String existingClass = element.attr("class");
                element.attr("class", (existingClass.isEmpty() ? "" : existingClass + " ") + className);
                element.removeAttr("style");
            }
        }

        // 4. 새로운 스타일 태그 생성 및 삽입
        Element newStyle = new Element("style").attr("type", "text/css");
        StringBuilder styleContent = new StringBuilder();
        styleContent.append("\n");

        for (Map.Entry<String, String> entry : styleMap.entrySet()) {
            styleContent.append(".")
                    .append(entry.getValue())
                    .append(" { ")
                    .append(entry.getKey())
                    .append(" }\n");
        }
        newStyle.html(styleContent.toString());
        head.appendChild(newStyle);

        // 5. 파일 저장
        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get("output.html")), StandardCharsets.UTF_8)) {
            writer.write(doc.outerHtml());
        }

        // 6. 결과 출력
        System.out.println("최적화 완료:");
        System.out.println("- 변환된 스타일 수: " + (styleCounter - 1));
        System.out.println("- 출력 파일: output.html");
    }
}