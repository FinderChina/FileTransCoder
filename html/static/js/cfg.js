// 获取DOM元素
const editorModal = document.getElementById('editorModal');
const textEditor = document.getElementById('textEditor');
const fileInput = document.getElementById('fileInput');
const mainCancelBtn = document.getElementById('mainCancelBtn');
const mainConfirmBtn = document.getElementById('mainConfirmBtn');
const lineNumbers = document.getElementById('lineNumbers');
const fileInfo = document.getElementById('fileInfo');
const statusMessage = document.getElementById('statusMessage');
const gotoLineBtn = document.getElementById('gotoLineBtn');
const viewHashBtn = document.getElementById('viewHashBtn');
const gotoLineModal = document.getElementById('gotoLineModal');
const gotoLineInput = document.getElementById('gotoLineInput');
const gotoLineCancelBtn = document.getElementById('gotoLineCancelBtn');
const gotoLineConfirmBtn = document.getElementById('gotoLineConfirmBtn');
const encodingBtn = document.getElementById('encodingBtn');
const encodingModal = document.getElementById('encodingModal');
const encodingSelect = document.getElementById('encodingSelect');
const encodingCancelBtn = document.getElementById('encodingCancelBtn');
const encodingConfirmBtn = document.getElementById('encodingConfirmBtn');
const loopCount = document.getElementById('loopCount');
const viewSize = document.getElementById('viewSize');
const autoTimes = document.getElementById('autoTimes');

const editorTab = document.getElementById('editorTab');
const configTab = document.getElementById('configTab');
const tabs = document.querySelectorAll('.modal-tab');

let currentFile = null;
let detectedEncoding = 'UTF-8';
let selectedEncoding = '';
let autoDdetectedEncoding = true;

//点击配置界面按钮
var cfgPanelShow = false;
$("#cfgBtn").on("click", function() {
    cfgPanelShow = !cfgPanelShow;
    if(cfgPanelShow){
        textEditor.value = cfgInitialEncoding;
        updateLineNumbers();
        loopCount.value = cfgLoopCount;
        autoTimes.value = cfgAutoTimes;
        editorModal.style.display = 'block';
    }else{
        cfgPanelHide();
    }
});

//隐藏配置界面
function cfgPanelHide(){
    setCfgVal();
    cfgPanelShow = false;
    editorModal.style.display = 'none';
}

//获取设置的值
function setCfgVal(){
    cfgLoopCount = loopCount.value;
    cfgAutoTimes = autoTimes.value;
    cfgInitialEncoding = textEditor.value;
}

//清空设置的值
function clearCfgVal(){
    cfgLoopCount = "";
    loopCount.value = "";
    cfgAutoTimes = "";
    autoTimes.value = "";
    cfgInitialEncoding = "";
    textEditor.value = "";
}

//文件内容转码开关
$("#convertChk").on("change", function() {
    if(this.checked) {
        $("#codeInput").show();
        cfgConvertFlag = true;
        loopCount.value = "20";
    }else{
        $("#codeInput").hide();
        cfgConvertFlag = false;
    }
});

// 循环次数鼠标滚轮事件处理
loopCount.addEventListener('wheel', function(e) {
    e.preventDefault();
    const delta = Math.sign(e.deltaY); // 获取滚轮方向
    const currentValue = parseInt(loopCount.value) || 0;
    var wheelVal = Math.min(Math.max(currentValue - delta, 1),200);   // 反向滚动更符合直觉
    loopCount.value = wheelVal;
});

//名称循环编码开关
$("#loopFileName").on("change", function() {
    if(this.checked) {
        cfgLoopFileName = true;
    }else{
        cfgLoopFileName = false;
    }
});

//列表等宽开关
$("#sameWidth").on("change", function() {
    if(this.checked) {
        eqwListFlag = true;
    }else{
        eqwListFlag = false;
    }
});

//自动切换开关
$("#autoSlide").on("change", function() {
    if(this.checked) {
        nxtAutoFlag  = true;
        $("#autoTimes").show();
    }else{
        nxtAutoFlag  = false;
        $("#autoTimes").hide();
    }
});


// 自动切换秒数鼠标滚轮事件处理
autoTimes.addEventListener('wheel', function(e) {
    e.preventDefault();
    const delta = Math.sign(e.deltaY); // 获取滚轮方向
    const currentValue = parseInt(autoTimes.value) || 0;
    var wheelVal = Math.min(Math.max(currentValue - delta, 1),60);   // 反向滚动更符合直觉
    autoTimes.value = wheelVal;
});

//打包下载
$("#pkgDownBtn").on("click", function(event) {
    exportZipFile();
});

//调整大小
$("#viewSizeBtn").on("click", function(event) {
    var cfgViewSize = viewSize.value;
    if(cfgViewSize==""){
        cfgViewSize="160*130";
    }
    resetPreviewSize(cfgViewSize);
});

//列表查看
$("#listViewBtn").on("click", function(event) {
    viewDivFile(!eqwListFlag);
    cfgPanelHide();
});

//自动播放
var intervalId;
$("#slideViewBtn").on("click", function(event) {
     if($('.imageDiv').length>0){
         photoSlide(null);
         if(nxtAutoFlag){
             var intervalVal = $("#autoTimes").val();
             var timeInterval = 3000;
             if(intervalVal!=""){
                 timeInterval = parseInt(intervalVal) * 1000;
             }
             if(Number.isNaN(timeInterval) || !timeInterval){
                 timeInterval = 3000;
             }
             clearTimeout(intervalId);
             intervalId = setInterval(function() {
                  swipeNext();
             }, timeInterval);
         }else{
             clearTimeout(intervalId);
         }
         cfgPanelHide();
     }else{
         showWaitMessageBox(" 没有图片可显示。 ", 3);;
     }
});

// 获取配置消息
function getCfgMsg(tip){
    setCfgVal();
    var msg = tip + ">>>CURRENT CONFIG<<<";;
    msg += "\n" + "文件转码(cfgConvertFlag) : " + cfgConvertFlag;
    msg += "\n" + "初始编码(cfgInitialEncoding) : " + (cfgInitialEncoding.length>10?(cfgInitialEncoding.substring(0,10) + "...(" + cfgInitialEncoding.length + ")"):cfgInitialEncoding); 
    msg += "\n" + "循环次数(cfgLoopCount) : " + cfgLoopCount + "K";
    msg += "\n" + "名称循环(cfgLoopFileName) : " + cfgLoopFileName;
    msg += "\n" + "缓存编码(fixedEncode) : " + (fixedEncode.length>10?(fixedEncode.substring(0,10) + "..."):fixedEncode) + "(" + fixedEncode.length + ")"; ;
    msg += "\n" + "列表等宽(eqwListFlag) : " + eqwListFlag;
    msg += "\n" + "自动切换(nxtAutoFlag) : " + nxtAutoFlag;
    msg += "\n" + "切换时间(cfgAutoTimes) : " + cfgAutoTimes;
    msg += "\n" + "文件数量(ImagesNum) : " + imgIdx + " - " + delIdx + " = " + (imgIdx - delIdx);
    return msg;
}



// 选项卡切换
tabs.forEach(tab => {
    tab.addEventListener('click', () => {
        // 更新选项卡样式
        tabs.forEach(t => t.classList.remove('active'));
        tab.classList.add('active');
        
        // 显示对应的内容
        const tabId = tab.getAttribute('data-tab');
        document.querySelectorAll('.modal-tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabId}Tab`).classList.add('active');
        
        // 如果是编辑器选项卡，更新行号
        if (tabId === 'editor') {
            updateLineNumbers();
        }
    });
});


// 自动加载选择的文件
fileInput.addEventListener('change', function(e) {
    if (fileInput.files.length > 0) {
        currentFile = fileInput.files[0];
        fileInfo.innerHTML = `文件: ${currentFile.name} (${formatFileSize(currentFile.size)})`;
        statusMessage.textContent = '';
        
        if(autoDdetectedEncoding){
            detectAndLoadFile(currentFile);
        }else{
            loadFileWithEncoding(currentFile, selectedEncoding);
        }
        
        e.target.value = ''; // 重置以允许重复选择
    }
});

// 检测并加载文件
function detectAndLoadFile(file) {
    const reader = new FileReader();
    
    // 先读取为二进制数组以检测编码
    reader.onload = function(e) {
        try {
            const rawData = new Uint8Array(e.target.result);
            const detection = jschardet.detect(rawData);
            //console.log(detection); 
            detectedEncoding = detection.encoding || 'UTF-8';
            
            loadFileWithEncoding(file, detectedEncoding);
        } catch (err) {
            statusMessage.textContent = '编码检测失败: ' + err.message;
            fileInfo.innerHTML = `文件: ${file.name} (${formatFileSize(file.size)})`;
        }
    };
    
    reader.onerror = function() {
        statusMessage.textContent = '文件读取失败';
    };
    
    reader.readAsArrayBuffer(file.slice(0, 1024 * 1024)); // 最多读取前1MB用于编码检测
}

// 使用指定编码加载文件
function loadFileWithEncoding(file, encoding) {
    const reader = new FileReader();
    
    reader.onload = function(e) {
        try {
            if (encoding.toUpperCase() === 'GBK') {
                // 使用iconv-lite处理GBK编码
                const gbkData = new Uint8Array(e.target.result);
                const text = iconv.decode(gbkData, 'gbk');  // window.iconvLite    iconv
                textEditor.value = text;
            } else if (encoding.toUpperCase() === 'UTF-8') {
                textEditor.value = e.target.result;
            } else if (encoding.toUpperCase() === 'UTF-16LE') {
                textEditor.value = e.target.result;
            } else if (encoding.toUpperCase() === 'UTF-16BE') {
                textEditor.value = e.target.result;
            } else if (encoding.toUpperCase() === 'ASCII') {
                textEditor.value = e.target.result;
            } else {
                // 尝试使用UTF-8，如果失败则使用原始二进制
                try {
                    textEditor.value = e.target.result;
                } catch (err) {
                    statusMessage.textContent = '自动解码失败，请尝试手动选择编码';
                }
            }
            
            updateLineNumbers();
            
            fileInfo.innerHTML = `文件: ${file.name} (${formatFileSize(file.size)}) | ${autoDdetectedEncoding?('检测编码: ' + detectedEncoding):('设置编码: ' + selectedEncoding)}`;
            autoDdetectedEncoding = true;
        } catch (err) {
            statusMessage.textContent = '解码失败: ' + err.message + '，请尝试手动选择编码';
        }
    };
    
    reader.onerror = function() {
        statusMessage.textContent = '文件读取失败';
    };
    
    // 尝试用指定的编码读取
    try {
        if (encoding.toUpperCase() === 'UTF-8') {
            reader.readAsText(file, 'UTF-8');
        } else if (encoding.toUpperCase() === 'UTF-16LE') {
            reader.readAsText(file, 'UTF-16LE');
        } else if (encoding.toUpperCase() === 'UTF-16BE') {
            reader.readAsText(file, 'UTF-16BE');
        } else if (encoding.toUpperCase() === 'ASCII') {
            reader.readAsText(file, 'UTF-8');
        } else {
            // 对于其他编码使用原始二进制打开
            reader.readAsArrayBuffer(file);
        }
    } catch (err) {
        reader.readAsArrayBuffer(file);  // readAsArrayBuffer  readAsText
    }
}

// 格式化文件大小
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// 更新行号
function updateLineNumbers() {
    const lines = textEditor.value.split('\n');
    let lineNumbersHtml = '';
    
    for (let i = 1; i <= lines.length; i++) {
        lineNumbersHtml += i + '<br>';
    }
    
    lineNumbers.innerHTML = lineNumbersHtml;
    
    // 同步滚动
    lineNumbers.scrollTop = textEditor.scrollTop;
}

// 文本编辑器滚动时同步行号滚动
textEditor.addEventListener('scroll', function() {
    lineNumbers.scrollTop = textEditor.scrollTop;
});

// 文本内容变化时更新行号
textEditor.addEventListener('input', function() {
    updateLineNumbers();
});

// 配置取消按钮
mainCancelBtn.addEventListener('click', function() {
    cfgPanelHide();
});

// 配置确认按钮
mainConfirmBtn.addEventListener('click', function() {
    var msg = getCfgMsg("");
    alert(msg);
});

// 显示文件编码选择界面
encodingBtn.addEventListener('click', function() {
    encodingModal.style.display = 'block';
});

// 编码确认按钮
encodingConfirmBtn.addEventListener('click', function() {
    selectedEncoding = encodingSelect.value;
    autoDdetectedEncoding = false;
    //detectedEncoding = selectedEncoding;
    //fileInfo.innerHTML = `文件: ${currentFile.name} (${formatFileSize(currentFile.size)}) | 使用编码: ${selectedEncoding}`;
    //loadFileWithEncoding(currentFile, selectedEncoding);
    encodingModal.style.display = 'none';
    statusMessage.textContent = '已手工设置编码：' + selectedEncoding;
    fileInput.click();  //  $('#fileInput').trigger('click');
});

// 文件编码双击自动选择对应的编码
function handleEncodingDblClick(){
    encodingConfirmBtn.click();
}

// 编码取消按钮
encodingCancelBtn.addEventListener('click', function() {
    encodingModal.style.display = 'none';
});

// 跳转到行按钮
gotoLineBtn.addEventListener('click', function() {
    gotoLineInput.value = '';
    gotoLineModal.style.display = 'block';
    gotoLineInput.focus();
});

// 跳转到行取消按钮
gotoLineCancelBtn.addEventListener('click', function() {
    gotoLineModal.style.display = 'none';
});

// 跳转到行确认按钮
gotoLineConfirmBtn.addEventListener('click', function() {
    const lineNumber = parseInt(gotoLineInput.value);
    if (!isNaN(lineNumber) && lineNumber > 0) {
        gotoLine(lineNumber);
    }
    gotoLineModal.style.display = 'none';
});

// 跳转到指定行
function gotoLine(lineNumber) {
    const lines = textEditor.value.split('\n');
    if (lineNumber > lines.length) {
        lineNumber = lines.length;
    }
    
    // 计算跳转位置
    let position = 0;
    for (let i = 0; i < lineNumber - 1; i++) {
        position += lines[i].length + 1; // +1 for newline character
    }
    
    // 设置光标位置并滚动到该位置
    textEditor.focus();
    textEditor.setSelectionRange(position, position);
    
    // 计算滚动位置
    const lineHeight = parseInt(window.getComputedStyle(textEditor).lineHeight);
    const editorHeight = textEditor.clientHeight;
    const scrollTo = (lineNumber - 1) * lineHeight - editorHeight / 2;
    textEditor.scrollTop = scrollTo;
}

// 查看哈希按钮
viewHashBtn.addEventListener('click', function() {
    var txt = textEditor.value;
    txt = txt.replace("\r\n", "\n");
    var hash = CryptoJS.SHA512(txt);
    var hashStr = hash.toString(CryptoJS.enc.Hex);
    alert(hashStr + "[" + hashStr.length + "]");
});


// 点击模态框外部关闭窗口
window.addEventListener('click', function(event) {
    if (event.target === editorModal) {
        cfgPanelHide();
    }
    if (event.target === gotoLineModal) {
        gotoLineModal.style.display = 'none';
    }
    if (event.target === encodingModal) {
        encodingModal.style.display = 'none';
    }
});

// 初始化行号
updateLineNumbers();