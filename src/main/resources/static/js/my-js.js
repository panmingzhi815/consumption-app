function selectRow(dataGrid,selectCallback,noSelectMsg) {
    var row = $(dataGrid).datagrid('getSelected');
    if(row) {
        selectCallback(row);
    }else if(noSelectMsg){
        $.messager.alert('提示',noSelectMsg,'warning');
    }
}

function post(url,data,callback) {
    $.ajax({
        'type' : 'POST',
        'url' : url,
        'contentType' : 'application/json',
        'dataType' : 'json',
        'data':JSON.stringify(data),
        'success' : function (data) {
            if(data.code === 200) {
                callback(data);
            }else{
                $.messager.alert('错误',data.codeMsg,"error");
                return false;
            }
        },
        "error":function () {
            $.messager.alert('错误',"服务器拒绝请求","error");
            return false;
        }
    });
}

function changePage(id) {
    $("#changeId").attr("href",id);
    $("#changeId").click();
}