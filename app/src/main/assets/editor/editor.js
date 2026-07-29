/**
 * Snippet Studio Offline Web Code Editor Driver
 * 100% 离线自包含的高性能代码编辑器内核驱动
 */

let textarea = null;
let gutter = null;
let container = null;
let isUpdatingFromNative = false;
let currentLineCount = 0;

document.addEventListener('DOMContentLoaded', () => {
    textarea = document.getElementById('code-input');
    gutter = document.getElementById('gutter');
    container = document.getElementById('textarea-container');

    if (!textarea || !gutter || !container) return;

    // 1. 监听文本变动事件
    textarea.addEventListener('input', () => {
        if (!isUpdatingFromNative) {
            updateGutter();
            notifyCodeChange();
        }
    });

    // 2. 监听光标与选中区域移动事件，实时计算行列号
    textarea.addEventListener('keyup', updateCursorPosition);
    textarea.addEventListener('click', updateCursorPosition);
    textarea.addEventListener('select', updateCursorPosition);

    // 3. 监听垂直滚动，使左侧行号轨与代码区完美同步滚动
    container.addEventListener('scroll', () => {
        gutter.scrollTop = container.scrollTop;
    });

    // 4. 通知 Android Native 网页离线编辑器就绪
    if (window.AndroidBridge) {
        window.AndroidBridge.onEditorReady();
    }
});

/** 更新行号轨道渲染 */
function updateGutter() {
    if (!textarea || !gutter) return;
    const lines = textarea.value.split('\n');
    const lineCount = lines.length;
    if (lineCount !== currentLineCount) {
        currentLineCount = lineCount;
        let gutterHtml = '';
        for (let i = 1; i <= lineCount; i++) {
            gutterHtml += `<div class="gutter-line" id="line-${i}">${i}</div>`;
        }
        gutterHtml += `<div style="height: 140px;"></div>`;
        gutter.innerHTML = gutterHtml;
    }
}

/** 实时计算当前光标行号与列号并回调 Native */
function updateCursorPosition() {
    if (!textarea || !window.AndroidBridge) return;
    const pos = textarea.selectionStart;
    const textBeforeCaret = textarea.value.substring(0, pos);
    const lines = textBeforeCaret.split('\n');
    const currentLine = lines.length - 1;
    const currentCol = lines[lines.length - 1].length;

    window.AndroidBridge.onCursorChanged(currentLine, currentCol);
}

/** 通知 Native 代码变动 */
function notifyCodeChange() {
    if (window.AndroidBridge && textarea) {
        window.AndroidBridge.onCodeChanged(textarea.value);
    }
}

// ===== Native 调用的对外接口通道 =====

/** 填充代码内容 */
function setCodeContent(code, language) {
    if (!textarea) return;
    isUpdatingFromNative = true;
    if (textarea.value !== code) {
        textarea.value = code;
        updateGutter();
    }
    isUpdatingFromNative = false;
}

/** 设置字号 */
function setFontSizeSp(fontSp) {
    if (!document.body || !textarea) return;
    document.body.style.fontSize = fontSp + 'px';
}

/** 设置自动换行 */
function setWordWrapEnabled(enabled) {
    if (!textarea) return;
    textarea.style.whiteSpace = enabled ? 'pre-wrap' : 'pre';
}

/** 设置主题 */
function setThemeIsDark(isDark) {
    if (!document.body || !gutter) return;
    if (isDark) {
        document.body.style.backgroundColor = '#121418';
        document.body.style.color = '#abb2bf';
        gutter.style.backgroundColor = '#1a1d24';
    } else {
        document.body.style.backgroundColor = '#ffffff';
        document.body.style.color = '#212529';
        gutter.style.backgroundColor = '#f8f9fa';
    }
}

/** 插入符号 */
function insertSymbolText(symbol) {
    if (!textarea) return;
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const text = textarea.value;
    textarea.value = text.substring(0, start) + symbol + text.substring(end);
    textarea.selectionStart = textarea.selectionEnd = start + symbol.length;
    textarea.focus();
    updateGutter();
    notifyCodeChange();
}
