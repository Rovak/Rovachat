"use strict";

var websocket = new WebSocket("ws://localhost:9000/chat/room");

websocket.onopen = function (ev) {
    console.log("websocket open");
    websocket.send(JSON.stringify({action: 'broadcast', message: 'all'}));
    websocket.send(JSON.stringify({action: 'channels'}));
};

websocket.onclose = function (ev) {

};


websocket.onerror = function (ev) {

};

function ChatChannelCtrl($scope, $routeParams) {
    console.log("Switched to channel: " + $routeParams.channelId);
    $scope.channel = $routeParams.channelId;

    $scope.onMessage = function() {
        websocket.send(JSON.stringify({
            'action' : 'channel',
            'channel' : $scope.channel,
            'message' : $scope.message
        }));
        $scope.message = "";
    }
}


angular.module('chat', []).
    config(['$routeProvider', function($routeProvider) {
      $routeProvider.when('/channel/:channelId', {
            controller: ChatChannelCtrl,
            templateUrl: 'assets/partials/channel.html'
        }).otherwise({redirectTo: '/channel'});
}]);


function ChannelCtrl($scope) {

    $scope.channels = [
        { title: 'Home' }
    ];

    console.log("asfdas");

    websocket.onmessage = function(ev) {

        var data = JSON.parse(ev.data);

        console.log("message", data);

        if (data.action) {
            switch(data.action) {
                case 'channels':
                    var channels = [];
                    for (var i = 0; i < data.channels.length; i++) {
                        channels.push({
                            title: data.channels[i].name
                        });
                    }

                    $scope.$apply(function(){
                        $scope.channels = channels;
                    });
                    break;
                case 'channel':
                    $('#channel-lines').append(data.channel + ": " + data.message + "<br>");
                    break;
            }
        }
    };
}

