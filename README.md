# 🧹 CssExtractor

**CssExtractor**는 HTML 문서 내 중복된 인라인 스타일을 추출하여 공통된 CSS 클래스로 변환하고, `<head>`의 `<style>` 태그에 통합함으로써 HTML 코드를 간결하고 유지보수 가능하게 만들어주는 도구입니다.

---

## ✨ 기능 소개

- HTML `<body>` 내부의 인라인 `style` 속성을 수집
- 동일한 스타일 속성을 가진 요소에 자동으로 공통 클래스(`.style-1`, `.style-2`, ...) 부여
- 기존 스타일은 제거하고 `<head>`의 `<style>` 태그에 통합
- 각 스타일 선언에 `!important`를 추가하여 우선순위 보장
- 중복 제거 및 파일 크기 최적화

---

## 📂 사용 방법

1. **HTML 파일 준비**
    - `input.htm`처럼 HTM 혹은 HTML 파일을 프로젝트 폴더에 준비합니다.
    - 인코딩은 **UTF-8** 형식을 권장합니다.
    - (인코딩이 UTF-8이 아닌 경우 메모장에서 저장할 때 "다른 이름으로 저장 → 인코딩: UTF-8" 선택 가능)

2. **파일 이름 지정**
    - `CssExtractor.java` 파일 내에서 아래 줄을 수정합니다:
      ```java
      File input = new File("_여기에 input 파일 넣어주세요._");
      ```

3. **프로그램 실행**
    - Java 프로젝트를 실행하면 `output.html` 파일이 자동으로 생성됩니다.
    - `output.html`에는 공통 스타일이 클래스 형태로 추출되어 적용된 HTML이 포함됩니다.

---

## 📄 예시

### 원본 HTML

```html
<td style="border: solid 1px black; padding: 5px; text-align: center;">내용</td>
```

### 변환 후 HTML
```html
<td class="style-1">내용</td>
```

### 변환 후 `<style>` 태그
```html
<style type="text/css">
<!--
.style-1 {
  border: solid 1px black !important;
  padding: 5px !important;
  text-align: center !important;
}
-->
</style>
```