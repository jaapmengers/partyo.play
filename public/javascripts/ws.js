var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
var chatSocket = new WS(jsRoutes.controllers.Application.takePicture().webSocketURL());