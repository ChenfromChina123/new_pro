package com.aispring.service.impl;

import com.aispring.entity.WordGameCourse;
import com.aispring.entity.WordGamePackage;
import com.aispring.entity.WordGamePackageClick;
import com.aispring.entity.WordGameProgress;
import com.aispring.entity.WordGameStatement;
import com.aispring.repository.WordGameCourseRepository;
import com.aispring.repository.WordGamePackageClickRepository;
import com.aispring.repository.WordGamePackageRepository;
import com.aispring.repository.WordGameProgressRepository;
import com.aispring.repository.WordGameStatementRepository;
import com.aispring.service.WordGameService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WordGameServiceImpl implements WordGameService {
    private static final String BUILTIN_PACKAGE_ID = "xingrong-beginner";

    private final WordGamePackageRepository packageRepository;
    private final WordGameCourseRepository courseRepository;
    private final WordGameStatementRepository statementRepository;
    private final WordGameProgressRepository progressRepository;
    private final WordGamePackageClickRepository clickRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPackages(Long userId, String search) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> builtinCourses = loadBuiltinCourses();
        if (matchSearch("星荣零基础学英语", "从零开始，系统掌握日常英语词汇与基础句型，适合完全零基础的学习者。", search)) {
            int totalQuestions = builtinCourses.stream()
                    .mapToInt(it -> asInt(it.get("count"), 0))
                    .sum();
            Map<String, Object> builtin = new LinkedHashMap<>();
            builtin.put("id", BUILTIN_PACKAGE_ID);
            builtin.put("name", "星荣零基础学英语");
            builtin.put("description", "从零开始，系统掌握日常英语词汇与基础句型，适合完全零基础的学习者。");
            builtin.put("icon", "🌟");
            builtin.put("level", "零基础");
            builtin.put("courseCount", builtinCourses.size());
            builtin.put("totalQuestions", totalQuestions);
            builtin.put("clickCount", getClickCount(BUILTIN_PACKAGE_ID));
            result.add(builtin);
        }

        List<WordGamePackage> userPackages = packageRepository.findVisiblePackages(userId, emptyToNull(search));
        for (WordGamePackage pkg : userPackages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", pkg.getId());
            item.put("name", pkg.getName());
            item.put("description", pkg.getDescription());
            item.put("icon", pkg.getIcon());
            item.put("level", pkg.getLevel());
            item.put("courseCount", courseRepository.findByPackageIdOrderByCourseIndexAsc(pkg.getId()).size());
            item.put("totalQuestions", statementRepository.countByPackageId(pkg.getId()));
            item.put("isUserPackage", true);
            item.put("isPublic", Boolean.TRUE.equals(pkg.getIsPublic()));
            item.put("clickCount", getClickCount(pkg.getId()));
            result.add(item);
        }

        result.sort((a, b) -> Integer.compare(asInt(b.get("clickCount"), 0), asInt(a.get("clickCount"), 0)));
        return result;
    }

    @Override
    @Transactional
    public void recordPackageClick(String packageId) {
        boolean exists = BUILTIN_PACKAGE_ID.equals(packageId) || packageRepository.existsById(packageId);
        if (!exists) {
            throw new IllegalArgumentException("课程包不存在");
        }
        WordGamePackageClick click = clickRepository.findById(packageId)
                .orElse(WordGamePackageClick.builder().packageId(packageId).clickCount(0).build());
        click.setClickCount((click.getClickCount() == null ? 0 : click.getClickCount()) + 1);
        clickRepository.save(click);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPackageCourses(Long userId, String packageId) {
        if (BUILTIN_PACKAGE_ID.equals(packageId)) {
            return loadBuiltinCourses();
        }
        WordGamePackage pkg = getVisiblePackage(userId, packageId);
        List<WordGameCourse> courses = courseRepository.findByPackageIdOrderByCourseIndexAsc(pkg.getId());
        List<Map<String, Object>> result = new ArrayList<>();
        for (WordGameCourse course : courses) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("index", course.getCourseIndex() + 1);
            item.put("title", course.getTitle());
            item.put("count", statementRepository.countByPackageIdAndCourseIndex(pkg.getId(), course.getCourseIndex()));
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCourseQuestions(Long userId, Integer courseIndex, String packageId) {
        if (courseIndex == null || courseIndex < 1) {
            throw new IllegalArgumentException("无效的课程索引");
        }
        String targetPackageId = (packageId == null || packageId.isBlank()) ? BUILTIN_PACKAGE_ID : packageId;
        if (BUILTIN_PACKAGE_ID.equals(targetPackageId)) {
            return loadBuiltinCourseQuestions(courseIndex);
        }
        WordGamePackage pkg = getVisiblePackage(userId, targetPackageId);
        List<WordGameStatement> rows = statementRepository.findByPackageIdAndCourseIndexOrderBySortOrderAscIdAsc(pkg.getId(), courseIndex - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (WordGameStatement row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("english", row.getEnglish());
            item.put("chinese", row.getChinese());
            item.put("soundmark", row.getSoundmark());
            result.add(item);
        }
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createPackage(Long userId, Map<String, Object> body) {
        String name = trimString(body.get("name"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("课程包名称不能为空");
        }
        String packageId = "up-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        WordGamePackage pkg = WordGamePackage.builder()
                .id(packageId)
                .userId(userId)
                .name(cut(name, 200))
                .description(cut(trimString(body.get("description")), 1000))
                .icon(cut(defaultIfBlank(trimString(body.get("icon")), "📦"), 5000))
                .level(cut(defaultIfBlank(trimString(body.get("level")), "自定义"), 50))
                .isPublic(asBoolean(body.get("isPublic")))
                .createdAt(LocalDateTime.now())
                .build();
        packageRepository.save(pkg);

        List<Map<String, Object>> sections = readSections(body);
        if (sections.isEmpty()) {
            sections = wrapLegacyStatements(body);
        }
        for (int i = 0; i < sections.size(); i++) {
            saveSection(packageId, i, sections.get(i));
        }
        return buildPackageMeta(pkg);
    }

    @Override
    @Transactional
    public Map<String, Object> addPackageSection(Long userId, String packageId, Map<String, Object> body) {
        WordGamePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("课程包不存在"));
        if (!Objects.equals(pkg.getUserId(), userId)) {
            throw new IllegalArgumentException("课程包不存在或无权操作");
        }
        List<Map<String, Object>> statements = readStatements(body.get("statements"));
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("至少需要一条题目");
        }
        int nextIndex = courseRepository.findFirstByPackageIdOrderByCourseIndexDesc(packageId)
                .map(c -> c.getCourseIndex() + 1)
                .orElse(0);
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("title", body.get("title"));
        section.put("statements", statements);
        saveSection(packageId, nextIndex, section);
        return buildPackageMeta(pkg);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getProgress(Long userId, String packageId) {
        if (packageId == null || packageId.isBlank()) {
            throw new IllegalArgumentException("packageId 不能为空");
        }
        List<WordGameProgress> rows = progressRepository.findByUserIdAndPackageId(userId, packageId);
        Map<String, Object> data = new LinkedHashMap<>();
        for (WordGameProgress row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("currentQuestion", row.getCurrentQuestion());
            item.put("completed", Boolean.TRUE.equals(row.getCompleted()));
            item.put("studySeconds", row.getStudySeconds());
            item.put("updatedAt", row.getUpdatedAt().toString());
            data.put(String.valueOf(row.getCourseIndex()), item);
        }
        return data;
    }

    @Override
    @Transactional
    public void saveProgress(Long userId, Map<String, Object> body) {
        String packageId = trimString(body.get("packageId"));
        Integer courseIndex = asIntObj(body.get("courseIndex"));
        if (packageId.isBlank() || courseIndex == null) {
            throw new IllegalArgumentException("packageId 和 courseIndex 必填");
        }
        WordGameProgress progress = progressRepository.findByUserIdAndPackageIdAndCourseIndex(userId, packageId, courseIndex)
                .orElse(WordGameProgress.builder()
                        .userId(userId)
                        .packageId(packageId)
                        .courseIndex(courseIndex)
                        .build());
        progress.setCurrentQuestion(asInt(body.get("currentQuestion"), 0));
        progress.setCompleted(asBoolean(body.get("completed")));
        progress.setStudySeconds(asInt(body.get("studySeconds"), 0));
        progress.setUpdatedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> migrateProgress(Long userId, Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("migrated", 0);
        result.put("message", "当前已统一登录态，无需迁移");
        return result;
    }

    private WordGamePackage getVisiblePackage(Long userId, String packageId) {
        WordGamePackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new IllegalArgumentException("课程包不存在"));
        if (!Boolean.TRUE.equals(pkg.getIsPublic()) && !Objects.equals(pkg.getUserId(), userId)) {
            throw new IllegalArgumentException("课程包不存在");
        }
        return pkg;
    }

    private void saveSection(String packageId, int courseIndex, Map<String, Object> section) {
        String defaultTitle = toChineseTitle(courseIndex + 1);
        String title = cut(defaultIfBlank(trimString(section.get("title")), defaultTitle), 200);
        WordGameCourse course = WordGameCourse.builder()
                .packageId(packageId)
                .courseIndex(courseIndex)
                .title(title)
                .createdAt(LocalDateTime.now())
                .build();
        courseRepository.save(course);

        List<Map<String, Object>> statements = readStatements(section.get("statements"));
        int order = 0;
        for (Map<String, Object> statement : statements) {
            String english = cut(trimString(statement.get("english")), 2000);
            String chinese = cut(trimString(statement.get("chinese")), 2000);
            if (english.isBlank() && chinese.isBlank()) {
                continue;
            }
            WordGameStatement row = WordGameStatement.builder()
                    .packageId(packageId)
                    .courseIndex(courseIndex)
                    .sortOrder(order++)
                    .english(english)
                    .chinese(chinese)
                    .soundmark(cut(trimString(statement.get("soundmark")), 200))
                    .build();
            statementRepository.save(row);
        }
    }

    private Map<String, Object> buildPackageMeta(WordGamePackage pkg) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", pkg.getId());
        item.put("name", pkg.getName());
        item.put("description", pkg.getDescription());
        item.put("icon", pkg.getIcon());
        item.put("level", pkg.getLevel());
        item.put("courseCount", courseRepository.findByPackageIdOrderByCourseIndexAsc(pkg.getId()).size());
        item.put("totalQuestions", statementRepository.countByPackageId(pkg.getId()));
        item.put("isUserPackage", true);
        item.put("isPublic", Boolean.TRUE.equals(pkg.getIsPublic()));
        item.put("clickCount", getClickCount(pkg.getId()));
        return item;
    }

    private int getClickCount(String packageId) {
        return clickRepository.findById(packageId).map(WordGamePackageClick::getClickCount).orElse(0);
    }

    private List<Map<String, Object>> loadBuiltinCourses() {
        List<Map<String, Object>> result = new ArrayList<>();
        Path dir = builtinCoursesDir();
        if (!Files.exists(dir)) {
            return result;
        }
        try {
            List<Path> files = Files.list(dir)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted(Comparator.comparingInt(path -> parseCourseIndex(path.getFileName().toString())))
                    .toList();
            for (Path file : files) {
                int index = parseCourseIndex(file.getFileName().toString());
                if (index <= 0) {
                    continue;
                }
                List<Map<String, Object>> statements = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", index);
                item.put("title", toChineseTitle(index));
                item.put("count", statements.size());
                result.add(item);
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private List<Map<String, Object>> loadBuiltinCourseQuestions(int index) {
        Path file = builtinCoursesDir().resolve(String.format("%02d.json", index));
        if (!Files.exists(file)) {
            throw new IllegalArgumentException("课程数据不存在");
        }
        try {
            List<Map<String, Object>> rows = objectMapper.readValue(file.toFile(), new TypeReference<>() {});
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("english", trimString(row.get("english")));
                item.put("chinese", trimString(row.get("chinese")));
                item.put("soundmark", trimString(row.get("soundmark")));
                result.add(item);
            }
            return result;
        } catch (IOException e) {
            throw new IllegalArgumentException("课程数据读取失败");
        }
    }

    private Path builtinCoursesDir() {
        return Path.of(System.getProperty("user.dir"), "..", "packages", "xingrong-courses", "data", "courses")
                .normalize()
                .toAbsolutePath();
    }

    private int parseCourseIndex(String fileName) {
        String pure = fileName.replace(".json", "");
        try {
            return Integer.parseInt(pure);
        } catch (Exception e) {
            return -1;
        }
    }

    private String toChineseTitle(int num) {
        String[] nums = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        if (num <= 10) {
            return "第" + nums[num] + "课";
        }
        int tens = num / 10;
        int ones = num % 10;
        String tensStr = tens == 1 ? "" : nums[tens];
        String onesStr = ones == 0 ? "" : nums[ones];
        return "第" + tensStr + "十" + onesStr + "课";
    }

    private List<Map<String, Object>> readSections(Map<String, Object> body) {
        Object sections = body.get("sections");
        if (!(sections instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    private List<Map<String, Object>> wrapLegacyStatements(Map<String, Object> body) {
        List<Map<String, Object>> statements = readStatements(body.get("statements"));
        if (statements.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("title", "第一课");
        first.put("statements", statements);
        List<Map<String, Object>> result = new ArrayList<>();
        result.add(first);
        return result;
    }

    private List<Map<String, Object>> readStatements(Object source) {
        if (!(source instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>((Map<String, Object>) map));
            }
        }
        return result;
    }

    private boolean matchSearch(String name, String description, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String term = search.toLowerCase(Locale.ROOT);
        return (name != null && name.toLowerCase(Locale.ROOT).contains(term))
                || (description != null && description.toLowerCase(Locale.ROOT).contains(term));
    }

    private String trimString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String cut(String value, int maxLen) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int asInt(Object value, int fallback) {
        Integer v = asIntObj(value);
        return v == null ? fallback : v;
    }

    private Integer asIntObj(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
