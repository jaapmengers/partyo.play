var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
var chatSocket = new WS(jsRoutes.controllers.Application.takePicture().webSocketURL());

var sendMessage = function(val) {
    chatSocket.send(JSON.stringify(
        {text: val}
    ));
};

$(document).ready(function(){
    $('#submit').on('click', function(){
      sendMessage($('#text').val());
      $('#text').val("")
    });
});

var receiveEvent = function(event) {
    var data = JSON.parse(event.data);
    $('#messages').append(data.message + '<br/>');
};

chatSocket.onmessage = receiveEvent;