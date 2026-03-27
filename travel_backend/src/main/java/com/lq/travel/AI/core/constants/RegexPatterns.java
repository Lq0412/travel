package com.lq.travel.AI.core.constants;

import java.util.regex.Pattern;

/**
 * 正则表达式模式常量
 * 预编译的正则表达式，提高性能并避免重复编译
 */
public class RegexPatterns {
    
    /**
     * 句子边界模式
     * 用于识别中文句子的结束标点（句号、感叹号、问号、换行）
     * 适用于流式输出的句子分割
     */
    public static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[。！？\n]");
    
    /**
     * 响应解析模式
     * 用于解析Agent的结构化响应（思考、行动、观察）
     * 支持多种分隔符：冒号、中文冒号、连字符
     */
    public static final Pattern RESPONSE_PARSE = Pattern.compile(
        "(?s)(?:思考[:：\\-]\\s*(.*?))?(?:行动[:：\\-]\\s*(.*?))?(?:观察[:：\\-]\\s*(.*))?$"
    );
    
    /**
     * 文本清理模式
     * 用于移除结构化标签（思考、行动、观察、推理）及其前缀
     * 适用于数字人对话等需要纯净文本的场景
     */
    public static final Pattern CLEAN_TEXT = Pattern.compile(
        "(?i)\\s*(思考|行动|观察|推理)[:：\\-]\\s*"
    );
    
    /**
    * 标签前缀模式
    * 用于移除身份前缀
     */
    public static final Pattern EMOJI_PREFIX = Pattern.compile("^\\s*(从化旅游助手[:：]?\\s*)?");
    
    /**
    * 身份标识模式
    * 用于移除从化旅游助手的身份标识
     */
    public static final Pattern IDENTITY_MARK = Pattern.compile("^\\s*(从化旅游助手[:：]?\\s*)");
    
    /**
     * 多余空格模式
     * 用于将多个连续空格替换为单个空格
     */
    public static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    
    // ==================== 私有构造函数 ====================
    
    /**
     * 私有构造函数，防止实例化
     */
    private RegexPatterns() {
        throw new AssertionError("RegexPatterns不应该被实例化");
    }
}

