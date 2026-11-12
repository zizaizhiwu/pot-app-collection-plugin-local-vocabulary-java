package com.zizaizhiwu.vocabulary.service;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 语言识别。
 * <p>
 * Pot 的收集插件只能拿到原文和译文，拿不到翻译方向，所以由服务端根据文本内容识别。
 * 生词本场景下文本通常很短（单词/短语），Lingua 对短文本的识别效果较好；
 * 这里限制候选语言集合，进一步降低误判。
 */
@Service
public class LanguageDetectionService {

    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    private static final LanguageDetector DETECTOR = LanguageDetectorBuilder
            .fromLanguages(
                    Language.ENGLISH,
                    Language.JAPANESE,
                    Language.CHINESE,
                    Language.KOREAN,
                    Language.FRENCH,
                    Language.GERMAN,
                    Language.SPANISH,
                    Language.RUSSIAN,
                    Language.PORTUGUESE,
                    Language.ITALIAN)
            .build();

    static {
        LABELS.put("ja", "日语");
        LABELS.put("zh", "中文");
        LABELS.put("en", "英语");
        LABELS.put("ko", "韩语");
        LABELS.put("fr", "法语");
        LABELS.put("de", "德语");
        LABELS.put("es", "西班牙语");
        LABELS.put("ru", "俄语");
        LABELS.put("pt", "葡萄牙语");
        LABELS.put("it", "意大利语");
    }

    /**
     * 识别文本语言，返回 ISO 639-1 代码；无法识别时返回空串。
     */
    public String detect(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        String normalized = text.trim();
        if (!containsLetter(normalized)) {
            return "";
        }
        // 纯 ASCII 文本统一按英语处理：单词/短语在多语言候选集里极易被误判成
        // 意大利语/葡萄牙语等拉丁语系，而本生词本的拉丁文本基本都是英文
        if (isAscii(normalized)) {
            return "en";
        }
        try {
            Language language = DETECTOR.detectLanguageOf(normalized);
            if (language == null) {
                return "";
            }
            // Lingua 1.x 用 IsoCode639_1 枚举表示两字母代码，toString() 即 "en"/"ja"/"zh"
            return language.getIsoCode639_1().toString().toLowerCase();
        } catch (Exception e) {
            // 纯数字/符号等无字母文本，Lingua 会抛异常，视为无法识别
            return "";
        }
    }

    /**
     * 识别原文语言。日文与中文短文本经常被混淆，
     * 按使用习惯统一把识别为中文的原文当作日文。
     */
    public String detectSource(String text) {
        String code = detect(text);
        return "zh".equals(code) ? "ja" : code;
    }

    /**
     * 语言代码对应的中文名称。
     */
    public String label(String code) {
        if (code == null || code.isEmpty()) {
            return "未识别";
        }
        String label = LABELS.get(code.toLowerCase());
        return label == null ? code.toUpperCase() : label;
    }

    private static boolean isAscii(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
