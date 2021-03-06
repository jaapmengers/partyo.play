var access_token;

// Put event listeners into place
window.addEventListener("DOMContentLoaded", function() {

	window.fbAsyncInit = function() {
		FB.init({
		  appId      : '231939803679691',
		  status     : true,
		  xfbml      : true
		});

		FB.getLoginStatus(function(response) {
			if(response.status !== 'connected'){
				FB.login(function(response){
				  access_token = response.authResponse.accessToken;
				}, {scope: 'user_events,photo_upload'});
			} else {
				access_token = response.authResponse.accessToken;
			}
		});
	};

	(function(d, s, id){
		var js, fjs = d.getElementsByTagName(s)[0];
		if (d.getElementById(id)) {return;}
		js = d.createElement(s); js.id = id;
		js.src = "//connect.facebook.net/en_US/all.js";
		fjs.parentNode.insertBefore(js, fjs);
	}(document, 'script', 'facebook-jssdk'));

	// Grab elements, create settings, etc.
	var canvas = document.getElementById("canvas"),
		context = canvas.getContext("2d"),
		video = document.getElementById("video"),
		videoObj = { "video": true },
		errBack = function(error) {
			console.log("Video capture error: ", error.code);
		};

		canvas.width = video.width;
		canvas.height = video.height;

	// Put video listeners into place
	if(navigator.getUserMedia) { // Standard
		navigator.getUserMedia(videoObj, function(stream) {
			video.src = stream;
			video.play();
		}, errBack);
	} else if(navigator.webkitGetUserMedia) { // WebKit-prefixed
		navigator.webkitGetUserMedia(videoObj, function(stream){
			video.src = window.webkitURL.createObjectURL(stream);
			video.play();
		}, errBack);
	}
	else if(navigator.mozGetUserMedia) { // Firefox-prefixed
		navigator.mozGetUserMedia(videoObj, function(stream){
			video.src = window.URL.createObjectURL(stream);
			video.play();
		}, errBack);
	}

	function takePicture() {

		console.log("Voor drawImage");
		context.drawImage(video, 0, 0, video.width, video.height);
		console.log("Na drawImage");

		if (!XMLHttpRequest.prototype.sendAsBinary) {
		  XMLHttpRequest.prototype.sendAsBinary = function (sData) {
		    var nBytes = sData.length, ui8Data = new Uint8Array(nBytes);
		    for (var nIdx = 0; nIdx < nBytes; nIdx++) {
		      ui8Data[nIdx] = sData.charCodeAt(nIdx) & 0xff;
		    }
		    /* send as ArrayBufferView...: */
		    this.send(ui8Data);
		    /* ...or as ArrayBuffer (legacy)...: this.send(ui8Data.buffer); */
		  };
		}

		var dataUrl = canvas.toDataURL("image/png");
		console.log("canvas.toDataURL")
		var encodedPng = dataUrl.substring(dataUrl.indexOf(',')+1,dataUrl.length);
		console.log("Voor decoding");
		var decodedPng = Base64Binary.decode(encodedPng);
		console.log("Na decoding");

		PostImageToFacebook(access_token, 'Is fissa', 'image/png', decodedPng, '');

	}

    window.chatSocket.onmessage = function(event){

        var kind = JSON.parse(event.data).kind;
        if(kind === 'takePicture')
            takePicture();
    };

}, false);



function PostImageToFacebook( authToken, filename, mimeType, imageData, message )
{
    // this is the multipart/form-data boundary we'll use
    var boundary = '----ThisIsTheBoundary1234567890';

    // let's encode our image file, which is contained in the var
    var formData = '--' + boundary + '\r\n'
    formData += 'Content-Disposition: form-data; name="source"; filename="' + filename + '"\r\n';
    formData += 'Content-Type: ' + mimeType + '\r\n\r\n';
    for ( var i = 0; i < imageData.length; ++i )
    {
        formData += String.fromCharCode( imageData[ i ] & 0xff );
    }
    formData += '\r\n';
    formData += '--' + boundary + '\r\n';
    formData += 'Content-Disposition: form-data; name="message"\r\n\r\n';
    formData += message + '\r\n'
    formData += '--' + boundary + '--\r\n';

    var xhr = new XMLHttpRequest();
    xhr.open( 'POST', 'https://graph.facebook.com/' + FACEBOOK_ID + '/photos?access_token=' + authToken, true );
    xhr.setRequestHeader( "Content-Type", "multipart/form-data; boundary=" + boundary );
		xhr.onreadystatechange = function() {
		if (xhr.readyState == 4 && xhr.status == 200) {
		  	console.log("Request send");
			}
		};
    xhr.sendAsBinary( formData );
    xhr = null;
}

var Base64Binary = {
	_keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

	/* will return a  Uint8Array type */
	decodeArrayBuffer: function(input) {
		var bytes = (input.length/4) * 3;
		var ab = new ArrayBuffer(bytes);
		this.decode(input, ab);

		return ab;
	},

	decode: function(input, arrayBuffer) {
		//get last chars to see if are valid
		var lkey1 = this._keyStr.indexOf(input.charAt(input.length-1));
		var lkey2 = this._keyStr.indexOf(input.charAt(input.length-2));

		var bytes = (input.length/4) * 3;
		if (lkey1 == 64) bytes--; //padding chars, so skip
		if (lkey2 == 64) bytes--; //padding chars, so skip

		var uarray;
		var chr1, chr2, chr3;
		var enc1, enc2, enc3, enc4;
		var i = 0;
		var j = 0;

		if (arrayBuffer)
			uarray = new Uint8Array(arrayBuffer);
		else
			uarray = new Uint8Array(bytes);

		input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

		for (i=0; i<bytes; i+=3) {
			//get the 3 octects in 4 ascii chars
			enc1 = this._keyStr.indexOf(input.charAt(j++));
			enc2 = this._keyStr.indexOf(input.charAt(j++));
			enc3 = this._keyStr.indexOf(input.charAt(j++));
			enc4 = this._keyStr.indexOf(input.charAt(j++));

			chr1 = (enc1 << 2) | (enc2 >> 4);
			chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
			chr3 = ((enc3 & 3) << 6) | enc4;

			uarray[i] = chr1;
			if (enc3 != 64) uarray[i+1] = chr2;
			if (enc4 != 64) uarray[i+2] = chr3;
		}

		return uarray;
	}
}