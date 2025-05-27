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
 *     공통된 Css를 임의의 명칭으로 묶어서 head 태그의 style로 묶어줍니다.
 *     이 때, 인라인으로 들어간 공통된 CSS는 제외됩니다.
 *     사용법:
 *      1. 프로젝트(CssExtractor) 폴더에 input으로 들어갈 html을 삽입합니다.
 *          (이 때, UTF-8로 된 파일을 사용하는 것을 추천드립니다.)
 *          (메모장에서 "다른 이름으로 저장"을 누르고 인코딩 방식을 바꿀 수 있습니다.)
 *      2. `File input = new File();` 안에 htm 혹은 html 파일명을 넣어줍니다.
 *      3. 프로그램을 실행시키면 output.html이 프로젝트 폴더에 생성됩니다.
 * </p>
 *
 * @author jeesw1221
 * @version 0.2 Beta
 *     2025.05.27
 */

public class CssExtractor {
    public static void main(String[] args) throws IOException {

        // 1. HTML 파일 읽기
        /*
        experiment03.htm 파일을 Jsoup을 이용해 파싱합니다.
         */
        File input = new File("experiment03.htm"); // <- 여기에 파일 이름 넣어주세요.
        Document doc = Jsoup.parse(input, "UTF-8");

        // 2. STYLE 태그 추적 및 관리
        /*
        기존 <style> 태그를 읽어두고,
        나중에 새로운 클래스 정의를 담을 <style> 엘리먼트를 새로 만듭니다.
         */
        Element head = doc.head();
        Element existingStyle = head.select("style").first();
        Element newStyle = new Element("style").attr("type", "text/css");

        // 3. 인라인 스타일 수집 및 분류
        /*
        1) <style="..."> 속성이 붙은 HTML 요소들을 전부 수집합니다.
        2) 동일한 스타일을 사용하는 요소들끼리 공통 클래스를 부여하기 위해 두 개의 맵을 사용합니다.
        3) 새로운 스타일 조각마다 style-1, style-2 같은 고유 클래스명을 자동 생성합니다.
        4) 인라인 스타일을 제거하고 대신 해당 클래스명을 class 속성에 넣습니다.
         */
        Map<String, String> styleMap = new LinkedHashMap<>();
        Map<String, Set<Element>> elementsByStyle = new HashMap<>();
        int styleCounter = 1;

        // body 내의 style 속성을 가진 요소들 처리
        Elements elementsWithStyle = doc.body().select("[style]");
        for (Element element : elementsWithStyle) {
            String style = element.attr("style").trim();
            if (!style.isEmpty()) {
                // 동일한 스타일이 이미 있는지 확인
                String className = styleMap.getOrDefault(style, null);
                if (className == null) {
                    // 새로운 스타일 클래스 생성
                    className = "style-" + styleCounter++;
                    styleMap.put(style, className);
                    elementsByStyle.put(className, new HashSet<>());
                }

                // 요소에 클래스 추가하고 style 속성 제거
                elementsByStyle.get(className).add(element);
                String existingClass = element.attr("class");
                element.attr("class", (existingClass.isEmpty() ? "" : existingClass + " ") + className);
                element.removeAttr("style");
            }
        }

        // 4. 스타일 태그 내용 생성
        /*
        1) 각 고유 스타일에 대해 CSS 클래스 정의를 생성합니다.
        2) !important를 붙여 기존 CSS 우선순위보다 높게 설정합니다.
        ex) color:red; → color:red !important; 식으로 변환합니다.
         */
        StringBuilder styleContent = new StringBuilder();
        styleContent.append("<!--\n");

        // 기존 스타일 복사
        if (existingStyle != null) {
            styleContent.append(existingStyle.html().replace("<!--", "").replace("-->", "").trim())
                    .append("\n\n/* Generated Styles */\n");
            existingStyle.remove();
        }

        // 새로운 스타일 추가
        for (Map.Entry<String, String> entry : styleMap.entrySet()) {
            String importantStyle = addImportantToStyle(entry.getKey());
            styleContent.append(".")
                    .append(entry.getValue())
                    .append(" { ")
                    .append(importantStyle)
                    .append(" }\n");
        }
        styleContent.append("-->");

        // 5. 새로운 스타일 태그 추가
        /*
        위에서 만든 클래스 정의들을 <style>에 담아 <head>에 삽입합니다.
         */
        newStyle.html(styleContent.toString());
        head.appendChild(newStyle);

        // 6. 파일 저장
        /*
        최적화된 HTML을 새 파일(output.html)에 저장합니다.
         */
        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get("output.html")), StandardCharsets.UTF_8)) {
            writer.write(doc.outerHtml());
        }

        // 7. 결과 출력
        System.out.println("최적화 완료:");
        System.out.println("- 변환된 스타일 수: " + (styleCounter - 1));
        System.out.println("- 출력 파일: output.html");
    }

    // !important 붙이는 함수 -> (한글 변환 시 기존 스타일보다 높은 우선 순위를 가져야 함)
    private static String addImportantToStyle(String style) {
        String[] declarations = style.split(";");
        StringBuilder result = new StringBuilder();
        for (String decl : declarations) {
            decl = decl.trim();
            if (!decl.isEmpty()) {
                if (!decl.contains("!important")) {
                    result.append(decl).append(" !important; ");
                } else {
                    result.append(decl).append("; ");
                }
            }
        }
        return result.toString().trim();
    }
}