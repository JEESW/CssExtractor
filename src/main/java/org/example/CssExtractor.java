package org.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *     공통된 Css를 임의의 명칭으로 묶어서 head 태그의 style로 묶어줍니다.
 *     이 때, 인라인으로 들어간 공통된 CSS는 제외됩니다.<br><br>
 *     동작 과정:<br>
 *          기존 style 태그 안에 있는 클래스를 각 요소에 인라인 스타일로 넣은 후,
 *          중복되는 인라인 스타일을 모아 class로 변환합니다.<br><br>
 *     사용법:<br>
 *      1. 프로젝트(CssExtractor) 폴더에 input으로 들어갈 html을 삽입합니다.
 *          (이 때, UTF-8로 된 파일을 사용하는 것을 추천드립니다.
 *          추가로 메모장에서 "다른 이름으로 저장"을 누르고 인코딩 방식을 바꿀 수 있습니다.)<br>
 *      2. `File input = new File();` 안에 htm 혹은 html 파일명을 넣어줍니다.<br>
 *      3. 프로그램을 실행시키면 output.html이 프로젝트 폴더에 생성됩니다.<br>
 * </p>
 *
 * @author jeesw
 * @version 0.4 Beta
 *     2025.05.28
 */

public class CssExtractor {
    public static void main(String[] args) throws IOException {

        // 1. HTML 파일 불러오기
        File input = new File("input.htm");
        if (!input.exists()) {
            throw new FileNotFoundException("파일을 찾을 수 없습니다: " + input.getAbsolutePath());
        }
        Document doc = Jsoup.parse(input, "UTF-8");

        // 2. 기존 <style> 태그에서 클래스명과 스타일을 추출 (ex. .HStyle0 → style)
        Element head = doc.head();
        Element existingStyle = head.select("style").first();
        Map<String, String> classToStyle = new HashMap<>();

        if (existingStyle != null) {
            String styleContent = existingStyle.html();

            // P.HStyle0, LI.HStyle0, DIV.HStyle0 등의 스타일을 추출하는 정규식
            Pattern pattern = Pattern.compile("(?:P|LI|DIV)\\.([^.\\s,{]+)\\s*\\{([^}]+)}");
            Matcher matcher = pattern.matcher(styleContent);
            while (matcher.find()) {
                String className = matcher.group(1).trim();
                String style = matcher.group(2).trim();
                classToStyle.put(className, style);
            }

            // 기존 style 태그 제거
            existingStyle.remove();
        }

        // 3. 각 요소에 클래스 기반 스타일을 인라인 스타일로 병합
        Elements allElements = doc.body().getAllElements();
        for (Element el : allElements) {
            String classAttr = el.className();
            if (!classAttr.isEmpty()) {
                StringBuilder mergedStyle = new StringBuilder();

                // 클래스 이름 하나씩 분리하여 해당하는 스타일 병합
                for (String cls : classAttr.split(" ")) {
                    if (classToStyle.containsKey(cls)) {
                        mergedStyle.append(classToStyle.get(cls)).append(" ");
                    }
                }

                // 기존 style 속성과 병합 (기존 style은 뒤에 붙임 → 우선순위 유지)
                String existingStyleAttr = el.attr("style").trim();
                if (mergedStyle.length() > 0) {
                    String finalStyle = mergedStyle.toString().trim();
                    if (!existingStyleAttr.isEmpty()) {
                        finalStyle += "; " + existingStyleAttr;
                    }
                    el.attr("style", finalStyle);
                }

                // class 속성 제거
                el.removeAttr("class");
            }
        }

        // 4. 동일한 인라인 스타일을 공통 클래스(Extracted1, Extracted2, ...)로 묶기
        Map<String, String> styleMap = new LinkedHashMap<String, String>(); // style → className
        Map<String, Set<Element>> elementsByStyle = new HashMap<String, Set<Element>>();
        int styleCounter = 1;

        Elements elementsWithStyle = doc.body().select("[style]");
        for (Element element : elementsWithStyle) {
            String style = element.attr("style").trim();
            if (!style.isEmpty()) {
                // 이미 등록된 스타일인지 확인
                String className = styleMap.get(style);
                if (className == null) {
                    className = "Extracted" + styleCounter++;
                    styleMap.put(style, className);
                    elementsByStyle.put(className, new HashSet<Element>());
                }

                // 요소에 class 속성 추가 후 style 제거
                elementsByStyle.get(className).add(element);
                String existingClass = element.attr("class");
                element.attr("class", (existingClass.isEmpty() ? "" : existingClass + " ") + className);
                element.removeAttr("style");
            }
        }

        // 5. 공통 클래스를 style 태그에 추가
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

        // 6. 결과 HTML 파일 저장
        try (Writer writer = new OutputStreamWriter(
            Files.newOutputStream(Paths.get("output.html")), StandardCharsets.UTF_8)) {
            writer.write(doc.outerHtml());
        }

        // 7. 완료 메시지 출력
        System.out.println("변환 완료:");
        System.out.println("- 변환된 스타일 수: " + (styleCounter - 1));
        System.out.println("- 출력 파일: output.html");
    }
}