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

var currentChannel = 'Public';

function ChatChannelCtrl($scope, $routeParams, $route) {

    $scope.onMessage = function() {
        websocket.send(JSON.stringify({
            'action' : 'channel',
            'channel' : currentChannel,
            'message' : $scope.message
        }));
        $scope.message = "";
    };
}

function ChannelCtrl($scope) {

    $scope.channels = [
        { title: 'Home' }
    ];

    $scope.switchChannel = function(channel) {
        currentChannel = channel;
        $('#channels .channel').hide();
        $('#room-' + channel).show();
        $('li.channel a.active').removeClass('active');
        $('li.channel a[channel="' + channel + '"]').addClass('active');
    };

    websocket.onmessage = function(ev) {

        var data = JSON.parse(ev.data);

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
                    console.log("recieved message!");
                    var divChannel = $('#room-' + data.channel);

                    if (!divChannel.length) {
                        $('#channels').append('<div class="channel" id="room-' + data.channel + '"></div>');
                        divChannel = $('#room-' + data.channel);
                    }

                    divChannel.append('<div class="message">' + data.message + "</div<");
                    divChannel.scrollTop(divChannel[0].scrollHeight);
                    break;
            }
        }
    };
}

