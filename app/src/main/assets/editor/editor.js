/**
 * Snippet Studio Code Editor JS Bridge & Engine Driver
 * 核心功能：初始化 Ace Editor 虚拟化内核，并建立与 Android Native (AndroidBridge) 的双向实时通信管道。
 */

let editor = null;
let isUpdatingFromNative = false;

document.addEventListener('DOMContentLoaded', () => {
    // 1. 初始化 Ace 代码编辑器
    editor = ace.edit("editor");
    
    // 2. 基础性能与体验配置 (开启虚拟滚动、末行超越留白、自动换行)
    editor.setOptions({
        theme: "ace/theme/one_dark",
        mode: "ace/mode/html",
        fontSize: "14px",
        showPrintMargin: false,
        wrap: true,                       // 开启自动软换行
        scrollPastEnd: 0.6,              // 允许越过最后一行滚动 (Scroll Beyond Last Line 缓冲区)
        fixedWidthGutter: true,
        useWorker: false,                // 禁用远程 Worker 下载避免纯离线报错
        behavioursEnabled: true,         // 开启自动成对补全括号
        animatedScroll: true
    });

    // 3. 监听文本修改变动事件，回调给 Android Native
    editor.session.on('change', () => {
        if (!isUpdatingFromNative && window.AndroidBridge) {
            const code = editor.getValue();
            window.AndroidBridge.onCodeChanged(code);
        }
    });

    // 4. 监听光标与选中区域移动事件，回调行列号给 Android Native 状态栏
    editor.selection.on('changeCursor', () => {
        if (window.AndroidBridge) {
            const pos = editor.getCursorPosition();
            window.AndroidBridge.onCursorChanged(pos.row, pos.column);
        }
    });

    // 5. 通知 Android Native 网页编辑器资源已就绪
    if (window.AndroidBridge) {
        window.AndroidBridge.onEditorReady();
    }
});

// ===== 供 Android Native (Kotlin WebView) 调用的 JS 函数通道 =====

/**
 * 填充代码内容并设置语言 Mode
 * @param {string} code 代码全文内容
 * @param {string} language 语言名称 (如 "html", "javascript", "markdown", "java", "css", "json")
 */
function setCodeContent(code, language) {
    if (!editor) return;
    isUpdatingFromNative = true;
    const currentCode = editor.getValue();
    if (currentCode !== code) {
        editor.setValue(code, -1); // -1 保持选区在最顶端
    }
    setLanguageMode(language);
    isUpdatingFromNative = false;
}

/**
 * 设置代码语法语言 Mode
 * @param {string} lang 语言 Key
 */
function setLanguageMode(lang) {
    if (!editor) return;
    const modeMap = {
        'html': 'ace/mode/html',
        'js': 'ace/mode/javascript',
        'javascript': 'ace/mode/javascript',
        'markdown': 'ace/mode/markdown',
        'md': 'ace/mode/markdown',
        'css': 'ace/mode/css',
        'java': 'ace/mode/java',
        'json': 'ace/mode/json',
        'prompt': 'ace/mode/text'
    };
    const targetMode = modeMap[lang.toLowerCase()] || 'ace/mode/text';
    editor.session.setMode(targetMode);
}

/**
 * 设置编辑器字体大小 (sp)
 * @param {number} fontSp 字体字号大小
 */
function setFontSizeSp(fontSp) {
    if (!editor) return;
    editor.setFontSize(`${fontSp}px`);
}

/**
 * 设置是否开启自动换行 (Word Wrap)
 * @param {boolean} enabled 是否开启
 */
function setWordWrapEnabled(enabled) {
    if (!editor) return;
    editor.session.setUseWrapMode(enabled);
}

/**
 * 设置主题 (深色 / 浅色)
 * @param {boolean} isDark 当前是否为深色主题
 */
function setThemeIsDark(isDark) {
    if (!editor) return;
    editor.setTheme(isDark ? "ace/theme/one_dark" : "ace/theme/chrome");
}

/**
 * 向当前光标位置插入快捷符号字符串
 * @param {string} symbol 符号内容 (如 "{}", "</>", "const ")
 */
function insertSymbolText(symbol) {
    if (!editor) return;
    editor.insert(symbol);
    editor.focus();
}
