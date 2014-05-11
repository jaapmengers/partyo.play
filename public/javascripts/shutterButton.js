$(document).ready(function(){

    var sendMessage = function(val) {
        window.chatSocket.send(JSON.stringify(
            {text: val}
        ));
    };

    $('#takePicture').on('click', function(){
      sendMessage('takePicture');
    });
});