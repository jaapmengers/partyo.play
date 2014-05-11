$(document).ready(function(){

  var deferred = Q.defer();

  window.fbAsyncInit = function() {
    FB.init({
      appId      : '231939803679691',
      status     : true,
      xfbml      : true
    });

    FB.getLoginStatus(function(response) {
      if(response.status !== 'connected'){
        FB.login(function(response){
          deferred.resolve(response.authResponse.accessToken);
        }, {scope: 'user_events,photo_upload'});
      } else {
        deferred.resolve(response.authResponse.accessToken);
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

  function selectEvent(id){
    var input = $('#facebookId').val(id);
    input.closest('form').submit();
  };

  deferred.promise.then(function(token){
    $.get("https://graph.facebook.com/v2.0/me/events?format=json&access_token=" + token, function(result){
      _.each(result.data, function(it){
        var item = $('<li/>').html(it.name);
        item.click(function(){
          selectEvent(it.id);
        });
        $('#events').append(item)
      });
    });
  });
});